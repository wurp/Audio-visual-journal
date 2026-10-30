<!-- Claude: this file is the setup guide for people. Follow CLAUDE.md, not this file. When starting a new app, the start-app skill replaces this file with a README for the app. -->

# Build Android apps from your phone with Claude

<!-- vibe-dev:start-link -->
## Next: start building

1. In the Claude app, open **Code** and tap **New session**.
2. Tap the repository name above the message box and choose **`Audio-visual-journal`**. The Claude app opens on whichever repository you used last, so always check this.
3. Send this message: `Start my app in Audio-visual-journal from its description.`

On a computer, [this link](https://claude.ai/code?repositories=wurp/Audio-visual-journal&prompt=Start%20my%20app%20in%20Audio-visual-journal%20from%20its%20description.) opens Claude with the repository and message already filled in.
<!-- /vibe-dev:start-link done -->

Describe the app you want in the Claude app. Claude writes it, GitHub builds it, and a few minutes later you install it on your phone from a link. Ask for changes the same way; each new version installs over the old one and keeps your data.

Your code lives in your own GitHub repository, and it uses your own Claude subscription. No other service holds your login.

## What you need

- An Android phone.
- A **Claude Pro or Max** plan. Claude Code in the cloud isn't on the Free plan. (With an API key, see [Using a computer or an API key](https://github.com/wurp/vibe-dev-template/wiki/Using-a-computer-or-an-API-key).)
- A free **GitHub** account.

## Set up (about 5 minutes)

1. **Create your app's repository.** Tap **[Create my app](https://github.com/new?template_owner=wurp&template_name=vibe-dev-template&owner=@me&visibility=private)**. Sign in to GitHub (or create a free account), give the repository a short name like `plant-diary`, optionally say what the app should do under **Description** (Claude starts from it), and tap **Create repository**. It's private: only you can see it. (To share the app with friends later, you can make it public.)
2. **Start building.** About a minute later, your new repository's page shows **Next: start building** at the top. In the Claude app, open **Code** → **New session**, tap the repository name above the message box and **choose your new repository** (the app opens on the one you used last), then send the message shown there. Approve GitHub access if Claude asks.
3. **Install it.** When Claude says it's ready, it gives you a link.
   - Long-press the link, choose **Copy link**, and paste it into **Chrome**, signed in to GitHub. (Downloads can stall inside the Claude app.)
   - If Chrome warns about the file, tap **Download anyway**, then tap the download.
   - The first time, Android asks you to allow installs from Chrome, and Google Play Protect asks you to confirm an app from an unknown developer. Both are normal for apps not from the Play Store.
   - Play Protect scans each new version before it installs. Just wait for it.

After that, tell Claude what to change. The link stays the same, and the new version installs over the old one.

## If something goes wrong

- **The app crashed:** open it again. It shows a **Copy for Claude** button. Tap it and paste the result to Claude.
- **Anything else:** see [Troubleshooting](https://github.com/wurp/vibe-dev-template/wiki/Troubleshooting).

## More

- [Setup in detail](https://github.com/wurp/vibe-dev-template/wiki/Setup-in-detail), including making the repository public and starting without the link.
- [Faster checks with one network setting](https://github.com/wurp/vibe-dev-template/wiki/Faster-checks).
- [How it works](https://github.com/wurp/vibe-dev-template/wiki/How-it-works), for developers.
- [Costs and limits](https://github.com/wurp/vibe-dev-template/wiki/Costs-and-limits).
- Questions and ideas: [Discussions](https://github.com/wurp/vibe-dev-template/discussions).

MIT licensed. Not affiliated with Anthropic.
