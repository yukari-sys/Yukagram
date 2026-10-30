#!/usr/bin/env python3

import os
import glob
import asyncio
import subprocess
from telegram import Bot

BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
COMMIT_ID = os.getenv("GIT_COMMIT_ID")
COMMIT_MESSAGE = os.getenv("GIT_COMMIT_MESSAGE")
CHANNEL_ID = "@yukagram"

async def main():
    async with Bot(BOT_TOKEN) as bot:
        files = glob.glob("./*.apk")
        files = [i for i in files if not i.endswith("-debug.apk")]        
        files = [i for i in files if not i.endswith("-google.apk")]

        caption = (
            f"**[{COMMIT_ID[:7]}]"
            f"(https://github.com/yukari-sys/Yukagram/commit/{COMMIT_ID})**\n\n"
            f"{COMMIT_MESSAGE}"
        )

        if not files:
            print("[fatal] no file to send")
            return

        print("[bot] sending files")
        await bot.send_document(
            chat_id=CHANNEL_ID,
            document=open(files[0], "rb"),
            caption=caption,
            parse_mode="Markdown",
        )

if __name__ == "__main__":
    asyncio.run(main())
