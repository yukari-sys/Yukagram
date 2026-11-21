package com.github.yukagram.ui;

import android.content.Context;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import org.drinkless.tdlib.TdApi;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibAccount;
import org.thunderdog.challegram.telegram.TdlibManager;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.EditBaseController;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.widget.MaterialEditTextGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.CRC32;

import me.vkryl.android.widget.FrameLayoutFix;

public class AuthKeyController extends EditBaseController<Void> implements SettingsAdapter.TextChangeListener {
  private static final String TAG = "AuthKeyImport";

  public AuthKeyController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_authkeyLogin;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.yu_YourAuthKey);
  }

  private SettingsAdapter adapter;
  private ListItem keyInput;
  private ListItem dcInput;
  private boolean inProgress;

  @Override
  protected void onCreateView(Context context, FrameLayoutFix contentView, RecyclerView recyclerView) {
    setDoneIcon(R.drawable.baseline_arrow_forward_24);
    setDoneVisible(false);

    this.adapter = new SettingsAdapter(this);
    adapter.setTextChangeListener(this);

    List<ListItem> items = adapter.getItems();

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(keyInput = new ListItem(ListItem.TYPE_EDITTEXT, R.id.edit_authkey, 0, R.string.yu_AuthKeyHex));
    items.add(dcInput = new ListItem(ListItem.TYPE_EDITTEXT, R.id.edit_dcId, 0, R.string.yu_DcId));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onTextChanged (int id, ListItem item, MaterialEditTextGroup v) {
    setDoneVisible(!keyInput.getStringValue().isEmpty() && !dcInput.getStringValue().isEmpty());
  }

  @Override
  protected boolean onDoneClick() {
    if (inProgress) {
      return false;
    }
    String input = keyInput != null ? keyInput.getStringValue() : "";
    input = input.trim().replaceAll("\\s+", "");
    int dcId;
    try {
      dcId = Integer.parseInt(dcInput != null ? dcInput.getStringValue().trim() : "");
    } catch (NumberFormatException e) {
      UI.showToast(R.string.yu_ErrorInvalidDcId, Toast.LENGTH_SHORT);
      return false;
    }
    byte[] authKey;
    try {
      authKey = decodeKey(input);
    } catch (IllegalArgumentException e) {
      UI.showToast(Lang.getString(R.string.yu_ErrorInvalidAuthKey) + ": " + e.getMessage(), Toast.LENGTH_SHORT);
      return false;
    }
    makeRequest(authKey, dcId);
    return true;
  }

  /*
  *
  * Verified as an OK code at  21 Nov 2026 (12:12:20) by Yukari
  *
  * */

  /**
   * Imports a raw 256-byte MTProto auth key as a new account.
   *
   * TDLib has no API for that, but it decides "logged in" purely from key/value records
   * inside the account's plaintext td.binlog (auth == "ok", auth&lt;dc&gt; == serialized
   * mtproto::AuthKey, main_dc_id), read by AuthManager/NetQueryDispatcher at startup.
   * We forge that file before TDLib ever opens the account directory, then create the
   * account: its TDLib client boots straight from our records, resolves my_id via get_me
   * and lands on authorizationStateReady.
   */
  public void makeRequest (byte[] authKey, int dcId) {
    if (authKey == null || authKey.length != 256) {
      UI.showToast(R.string.yu_ErrorAuthKey256Bytes, Toast.LENGTH_SHORT);
      return;
    }
    if (dcId < 1 || dcId > 5) {
      UI.showToast(R.string.yu_ErrorDCBetween1and5, Toast.LENGTH_SHORT);
      return;
    }
    if (inProgress) {
      return;
    }

    // Predict which account id TdlibManager.newAccount(false) will produce:
    // it reuses the first (lowest id) unauthorized non-current, non-service account,
    // otherwise it appends one with id == accounts.size(). The file must be written
    // BEFORE newAccount runs, because creating the account starts its TDLib client,
    // which opens td.binlog immediately (ClientHolder ctor -> init -> SetTdlibParameters).
    TdlibManager manager = tdlib.context();
    int currentId = manager.currentAccount().id; // exactly what newAccount excludes
    int accountsCount = 0;
    int reusableId = TdlibAccount.NO_ID;
    // NOTE: do NOT use manager.iterator() here - it filters out unauthorized accounts,
    // which are exactly the ones newAccount reuses. account(id) returns accounts.get(id),
    // so walking ids 0,1,2,... reproduces newAccount's list-order scan exactly.
    while (manager.hasAccount(accountsCount)) {
      TdlibAccount account = manager.account(accountsCount);
      accountsCount++;
      if (account.id != currentId && account.isUnauthorized() && !account.isLoggingOut() && account.tdlibInstanceMode() != Tdlib.Mode.SERVICE) {
        reusableId = account.id; // newAccount returns the first match in list order
        break;
      }
    }
    final int targetId = reusableId != TdlibAccount.NO_ID ? reusableId : accountsCount;
    if (targetId >= TdlibAccount.ID_MAX) {
      UI.showToast(R.string.yu_ErrorTooManyAccounts, Toast.LENGTH_SHORT);
      return;
    }
    if (reusableId != TdlibAccount.NO_ID && manager.account(reusableId).hasTdlib(false)) {
      // That account's TDLib already has td.binlog open - can't safely rewrite it.
      UI.showToast(R.string.yu_ErrorPendingAccount, Toast.LENGTH_SHORT);
      return;
    }

    final File binlog;
    try {
      binlog = new File(TdlibManager.getTdlibDirectory(targetId, false), "td.binlog");
      if (binlog.exists() && !binlog.delete()) {
        throw new IOException("cannot remove existing td.binlog");
      }
      writeBinlog(binlog, dcId, authKey);
    } catch (Throwable t) {
      UI.showToast(Lang.getString(R.string.yu_ErrorAuthKeyLoginFailed) + ": " + t.getMessage(), Toast.LENGTH_SHORT);
      return;
    }

    int createdId;
    try {
      createdId = manager.newAccount(false);
    } catch (Throwable t) {
      createdId = TdlibAccount.NO_ID;
    }
    if (createdId != targetId) {
      // Predicted wrong (e.g. the reusable account was logging out) - remove our
      // records so no account boots from them by accident.
      //noinspection ResultOfMethodCallIgnored
      binlog.delete();
      UI.showToast(R.string.yu_ErrorAuthKeyLoginFailed, Toast.LENGTH_SHORT);
      return;
    }

    inProgress = true;
    final Tdlib target = TdlibManager.getTdlib(createdId);
    final long start = SystemClock.uptimeMillis();
    final boolean[] success = {false};
    final boolean[] restarted = {false};
    final boolean[] sawNotReady = {false};
    final long[] restartAt = {0};
    UI.post(new Runnable() {
      @Override
      public void run () {
        long elapsed = SystemClock.uptimeMillis() - start;
        TdApi.AuthorizationState state = target.authorizationState();
        boolean ready = state != null && state.getConstructor() == TdApi.AuthorizationStateReady.CONSTRUCTOR;
        long myUserId = target.myUserId();
        if (!success[0]) {
          if (!restarted[0]) {
            if (myUserId != 0) {
              // get_me succeeded: TDLib has persisted the pmc "my_id" record. The FIRST
              // boot of a forged session never creates dialog folders:
              // MessagesManager::init() sees was_authorized() == false (without my_id,
              // AuthManager boots asynchronously via get_me), and the only other
              // create_folders() caller, on_authorization_success(), runs solely on the
              // sign-in path. The first dialog then trips
              // LOG_CHECK(folder_ptr != nullptr) in set_dialog_order and kills the
              // process. Close the client right now: the reboot finds auth=ok + my_id,
              // boots synchronously authorized and creates the folders in init().
              restarted[0] = true;
              restartAt[0] = elapsed;
              Log.i(TAG, "my_id known after " + elapsed + "ms, restarting TDLib client");
              target.stressTest(1);
            } else if (elapsed >= 20_000 || (ready && elapsed >= 5_000)) {
              // ready with myUserId == 0 means get_me failed (bad key, swallowed by
              // AuthManager), never ready means the key/DC didn't work at all
              Log.w(TAG, "pre-restart timeout: ready=" + ready + " myUserId=" + myUserId + " elapsed=" + elapsed + "ms");
              rollbackImport(target, binlog, "no my_id");
              return;
            }
          } else {
            if (!ready) {
              sawNotReady[0] = true; // client went down after the restart request
              Log.i(TAG, "restart observed: state=" + state + " after " + (elapsed - restartAt[0]) + "ms");
            }
            boolean healthy = ready && myUserId != 0;
            if (healthy && (sawNotReady[0] || elapsed - restartAt[0] >= 5_000)) {
              // back to Ready on the SECOND boot: folders exist, safe to show chats.
              // If the non-Ready gap was never observed (slow poll), still accept a
              // healthy Ready+my_id after 5s - rolling back a working import is worse
              // than switching slightly early.
              success[0] = true;
              Log.i(TAG, "import confirmed (sawNotReady=" + sawNotReady[0] + ", " + (elapsed - restartAt[0]) + "ms after restart)");
              UI.showToast(R.string.yu_LoggedInWithAuthKey, Toast.LENGTH_SHORT);
            } else if (elapsed - restartAt[0] >= 25_000 && !healthy) {
              Log.w(TAG, "post-restart timeout: state=" + state + " myUserId=" + myUserId);
              rollbackImport(target, binlog, "restart did not complete");
              return;
            }
          }
        }
        if (success[0]) {
          if (target.context().preferredAccountId() == target.id()) {
            inProgress = false;
            return;
          }
          if (elapsed >= 30_000) {
            // Account is authorized anyway, just not switched to
            inProgress = false;
            return;
          }
          // changePreferredAccountId rejects accounts that are still flagged
          // unauthorized; the flag flips right after Ready, so retry briefly
          target.context().changePreferredAccountId(target.id(), TdlibManager.SWITCH_REASON_USER_CLICK, _ -> {});
        }
        UI.post(this, restarted[0] ? 75 : 100);
      }
    }, 100);
  }

  private void rollbackImport (Tdlib target, File binlog, String reason) {
    Log.w(TAG, "rolling back import: " + reason);
    inProgress = false;
    UI.showToast(R.string.yu_ErrorAuthKeyLoginFailed, Toast.LENGTH_SHORT);
    try {
      //noinspection ResultOfMethodCallIgnored
      binlog.delete();
    } catch (Throwable ignored) {
    }
    try {
      target.eraseTdlibDatabase(ok -> {
      });
    } catch (Throwable ignored) {
    }
  }


  // Binlog forging, coupled to TDLib v1.8.68 (tdlib/builder/td @ c15d3f5a)

  private static final int BINLOG_PMC_MAGIC = 0x4327; // LogEvent::HandlerType::BinlogPmcMagic
  private static final int EVENT_HEADER_SIZE = 4 + 8 + 4 + 4 + 8; // size, id, type, flags, extra
  private static final int EVENT_TAIL_SIZE = 4; // crc32

  private static byte[] decodeKey (String input) {
    if (input.isEmpty()) {
      throw new IllegalArgumentException("key is empty");
    }
    if (input.length() == 512 && input.matches("[0-9a-fA-F]+")) {
      byte[] key = new byte[256];
      for (int i = 0; i < 256; i++) {
        key[i] = (byte) Integer.parseInt(input.substring(i * 2, i * 2 + 2), 16);
      }
      return key;
    }
    byte[] key;
    try {
      key = Base64.decode(input, Base64.DEFAULT);
    } catch (IllegalArgumentException first) {
      try {
        key = Base64.decode(input, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
      } catch (IllegalArgumentException second) {
        throw new IllegalArgumentException("not valid base64 or hex");
      }
    }
    if (key == null || key.length != 256) {
      throw new IllegalArgumentException("key must decode to 256 bytes, got " + (key == null ? 0 : key.length));
    }
    return key;
  }

  private static void writeBinlog (File file, int dcId, byte[] authKey) throws IOException {
    byte[][] events = {
      makePmcEvent(1, bytes("auth"), bytes("ok")),
      makePmcEvent(2, bytes("auth" + dcId), serializeAuthKey(authKey)),
      makePmcEvent(3, bytes("main_dc_id"), bytes(Integer.toString(dcId))),
    };
    File parent = file.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("cannot create " + parent.getPath());
    }
    try (FileOutputStream out = new FileOutputStream(file)) {
      for (byte[] event : events) {
        out.write(event);
      }
      out.flush();
      out.getFD().sync();
    }
    // Malformed records make TDLib LOG(FATAL) on load - never hand over unverified data
    verifyBinlog(file, dcId, authKey);
  }

  /** Event = int32 size | int64 id | int32 type | int32 flags | int64 extra | data | int32 crc32 */
  private static byte[] makePmcEvent (long id, byte[] key, byte[] value) {
    byte[] keyData = tlString(key);
    byte[] valueData = tlString(value);
    int size = EVENT_HEADER_SIZE + keyData.length + valueData.length + EVENT_TAIL_SIZE;
    ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    buf.putInt(size);
    buf.putLong(id);
    buf.putInt(BINLOG_PMC_MAGIC);
    buf.putInt(0); // flags: fresh key, no Rewrite
    buf.putLong(0); // extra
    buf.put(keyData);
    buf.put(valueData);
    CRC32 crc = new CRC32();
    crc.update(buf.array(), 0, size - EVENT_TAIL_SIZE);
    buf.putInt((int) crc.getValue());
    return buf.array();
  }

  /** Classic TL string: 1-byte len (<254) or 0xFE + 3-byte little-endian len, padded to 4. */
  private static byte[] tlString (byte[] data) {
    int len = data.length;
    int prefix = len < 254 ? 1 : 4;
    byte[] out = new byte[(prefix + len + 3) & ~3];
    if (len < 254) {
      out[0] = (byte) len;
      System.arraycopy(data, 0, out, 1, len);
    } else {
      out[0] = (byte) 254;
      out[1] = (byte) (len & 0xff);
      out[2] = (byte) ((len >> 8) & 0xff);
      out[3] = (byte) ((len >> 16) & 0xff);
      System.arraycopy(data, 0, out, 4, len);
    }
    return out;
  }

  /** mtproto::AuthKey store(): int64 key_id | int32 flags | tlString(key) | double created_at */
  private static byte[] serializeAuthKey (byte[] authKey) {
    byte[] keyString = tlString(authKey); // 256 bytes -> 0xFE prefix, 260 bytes total
    ByteBuffer buf = ByteBuffer.allocate(8 + 4 + keyString.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buf.putLong(computeKeyId(authKey));
    buf.putInt(1 | 4); // AUTH_FLAG | HAS_CREATED_AT; AUTH_FLAG is required for AuthKeyState::OK
    buf.put(keyString);
    buf.putDouble(System.currentTimeMillis() / 1000.0);
    return buf.array();
  }

  /** DhHandshake::calc_key_id: little-endian int64 over SHA-1(key)[12..20) */
  private static long computeKeyId (byte[] authKey) {
    try {
      byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(authKey);
      long keyId = 0;
      for (int i = 12; i < 20; i++) {
        keyId |= ((long) (sha1[i] & 0xff)) << (8 * (i - 12));
      }
      return keyId;
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static void verifyBinlog (File file, int dcId, byte[] authKey) throws IOException {
    byte[] data = readFile(file);
    HashMap<String, byte[]> map = new HashMap<>();
    int pos = 0;
    long expectedId = 1;
    while (pos < data.length) {
      if (data.length - pos < EVENT_HEADER_SIZE + EVENT_TAIL_SIZE) {
        throw new IOException("truncated event header");
      }
      ByteBuffer header = ByteBuffer.wrap(data, pos, EVENT_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
      int size = header.getInt();
      long id = header.getLong();
      int type = header.getInt();
      header.getInt(); // flags
      header.getLong(); // extra
      if (size < EVENT_HEADER_SIZE + EVENT_TAIL_SIZE || (size & 3) != 0 || size > data.length - pos) {
        throw new IOException("bad event size " + size);
      }
      if (id != expectedId++) {
        throw new IOException("bad event id " + id);
      }
      if (type != BINLOG_PMC_MAGIC) {
        throw new IOException("bad event type 0x" + Integer.toHexString(type));
      }
      CRC32 crc = new CRC32();
      crc.update(data, pos, size - EVENT_TAIL_SIZE);
      int storedCrc = ByteBuffer.wrap(data, pos + size - EVENT_TAIL_SIZE, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
      if ((int) crc.getValue() != storedCrc) {
        throw new IOException("crc mismatch at event " + id);
      }
      int dataStart = pos + EVENT_HEADER_SIZE;
      int dataEnd = pos + size - EVENT_TAIL_SIZE;
      int[] cursor = {dataStart};
      byte[] key = readTlString(data, cursor, dataEnd);
      byte[] value = readTlString(data, cursor, dataEnd);
      if (cursor[0] != dataEnd) {
        throw new IOException("trailing bytes in event data");
      }
      map.put(new String(key, StandardCharsets.UTF_8), value);
      pos += size;
    }

    byte[] auth = map.get("auth");
    if (auth == null || !Arrays.equals(auth, bytes("ok"))) {
      throw new IOException("missing auth=ok record");
    }
    byte[] mainDc = map.get("main_dc_id");
    if (mainDc == null || !Arrays.equals(mainDc, bytes(Integer.toString(dcId)))) {
      throw new IOException("missing main_dc_id record");
    }
    byte[] storedKey = map.get("auth" + dcId);
    if (storedKey == null) {
      throw new IOException("missing auth" + dcId + " record");
    }
    ByteBuffer keyBuf = ByteBuffer.wrap(storedKey).order(ByteOrder.LITTLE_ENDIAN);
    long keyId = keyBuf.getLong();
    int flags = keyBuf.getInt();
    if ((flags & 1) == 0) {
      throw new IOException("AUTH_FLAG not set");
    }
    int[] cursor = {keyBuf.position()};
    byte[] storedKeyBytes = readTlString(storedKey, cursor, storedKey.length);
    if (!Arrays.equals(storedKeyBytes, authKey)) {
      throw new IOException("stored key differs from input");
    }
    if (cursor[0] + 8 > storedKey.length) {
      throw new IOException("missing created_at");
    }
    double createdAt = ByteBuffer.wrap(storedKey, cursor[0], 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    if (createdAt <= 0) {
      throw new IOException("bad created_at");
    }
    if (keyId != computeKeyId(authKey)) {
      throw new IOException("bad auth key id");
    }
    if (cursor[0] + 8 != storedKey.length) {
      throw new IOException("trailing bytes in auth key record");
    }
  }

  private static byte[] readTlString (byte[] src, int[] cursor, int limit) throws IOException {
    int start = cursor[0];
    if (start >= limit) {
      throw new IOException("unexpected end of data");
    }
    int first = src[start] & 0xff;
    int len;
    int prefix;
    if (first < 254) {
      len = first;
      prefix = 1;
    } else if (first == 254) {
      if (start + 4 > limit) {
        throw new IOException("truncated string length");
      }
      len = (src[start + 1] & 0xff) | ((src[start + 2] & 0xff) << 8) | ((src[start + 3] & 0xff) << 16);
      prefix = 4;
    } else {
      throw new IOException("unsupported string length marker " + first);
    }
    int end = start + prefix + len;
    if (end > limit) {
      throw new IOException("string exceeds limit");
    }
    int next = start + ((prefix + len + 3) & ~3);
    if (next > limit) {
      throw new IOException("string padding exceeds limit");
    }
    cursor[0] = next;
    return Arrays.copyOfRange(src, start + prefix, end);
  }

  private static byte[] readFile (File file) throws IOException {
    byte[] data = new byte[(int) file.length()];
    try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
      int offset = 0;
      while (offset < data.length) {
        int read = in.read(data, offset, data.length - offset);
        if (read < 0) {
          throw new IOException("unexpected end of file");
        }
        offset += read;
      }
    }
    return data;
  }

  private static byte[] bytes (String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }
}
