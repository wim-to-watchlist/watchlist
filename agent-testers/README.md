# AI testers

A handful of AI testers, each on its own Android emulator on GitHub Actions, use Watchlist
like a new user would. Each one plays a persona (a Dutch nurse on a Pixel 7, a German on
an old small phone with large text who refuses every permission, someone at 200% font, a
Spanish teacher on a tablet, …), signs in with a SIMKL test account, tries to reach a goal
and writes a bug report with screenshots. At the end, one summary lists every issue, most
severe first, with which testers ran into it.

Based on [mobile-agent-testers](https://github.com/pierridotite/mobile-agent-testers)
(MIT, see `LICENSE`), cut down to Android and to this private repository.

## What it costs

- **GitHub Actions minutes.** This repository is private, so the runs use the 2,000 free
  minutes a month (Linux only, counted once). A tester takes about 15 to 30 minutes
  including the emulator start, plus about 5 minutes to build the app. A run with 4
  testers is roughly 70 to 130 minutes. Nothing runs by itself: only when you start it.
- **Claude usage.** Each tester is a Claude Code agent. With a Claude subscription, 20
  agents at once used up a 5-hour window in about 15 minutes, so a run is limited to 6
  testers at a time. With an API key instead, a tester costs about $0.15 to $0.55.

## One-time setup

All of it is done on github.com, in the repository's **Settings › Secrets and variables ›
Actions › New repository secret**. Nobody else, Claude included, needs to see these values.

1. **A SIMKL test account.** Make a new, separate SIMKL account (not your own: the testers
   mark episodes and rate titles in it). Add a few shows (some with unwatched episodes, some
   airing this week), a few movies and an anime, so the testers have something to work
   with. Then add two secrets:
   - `SIMKL_TEST_EMAIL`: the account's e-mail address
   - `SIMKL_TEST_PASSWORD`: its password
2. **Claude access**, one of the two:
   - With your Claude subscription: on a computer with Claude Code installed, run
     `claude setup-token` in a terminal, sign in, and copy the token it shows (it starts with
     `sk-ant-oat`). Add it as the secret `CLAUDE_CODE_OAUTH_TOKEN`.
   - Or with an API key (pay per use): create a key at console.anthropic.com and add it as
     the secret `ANTHROPIC_API_KEY`.

The `SIMKL_CLIENT_ID` variable the app needs is already set for CI.

## Running it

1. Open **Actions › Agent testers › Run workflow**.
2. Leave the defaults (4 testers: sanne, lukas, camille, grace) or type other names from
   `personas.json`, separated by commas. At most 6 at a time.
3. **Run workflow.** It takes 30 to 45 minutes.

Other options on the same form:

- **ref**: test a branch or a pull request's branch instead of `main`.
- **brief**: `briefs/improve.md` asks for an improvement report (where the tester
  hesitated and what to change first) instead of a bug hunt.
- **bug_report**: paste the steps of a bug, or an issue like `wim-to-watchlist/watchlist#12`,
  and each tester tries to reproduce it on its phone and answers yes, no or unclear.
- **model**: `haiku` is cheaper, `opus` more thorough.

## Reading the results

When the run is done, open it: the summary (every issue, most severe first, and how many
testers hit it) is right on the run page. Under **Artifacts**, `all-reports` has the same
summary plus each tester's full report and numbered screenshots.

AI testers sometimes misread a screen. Look at the screenshot of an issue before you
believe it, or ask Claude to go through the reports and check them.

## Good to know

- The testers use the debug build, "Watchlist Dev", where Watchlist Plus is unlocked. The
  paywall and Google Play buying are not tested. Cast and crew stay empty (no TMDB key).
- They sign in through Chrome on simkl.com. If SIMKL shows a captcha to the emulator, the
  testers stop and report it as outside the app.
- The testers share the one test account, so they may see each other's changes. That is why
  a run takes at most 6 at a time and only one run goes at once.
- Reports and screenshots are not encrypted, because this repository is private: only
  people with access to it can see them. The run refuses to start if the repository is
  public, unless you tick **allow_public** on the Run workflow form: then this run's reports
  are visible to anyone until the repository is made private.

## Files

| File | What it is |
|---|---|
| `../.github/workflows/agent-testers.yml` | The workflow: build, one emulator per tester, summary |
| `personas.json` | The testers: who they are, their goal, language and phone |
| `AGENT_BRIEF.md` | Instructions every tester follows: tools, rules, how to sign in, the report |
| `briefs/` | Other missions: an improvement report, reproducing a bug |
| `scripts/` | Screenshots and screen reading, typing, the job list and the summary |
| `tests/` | Tests for the scripts: `python3 -m unittest discover -s agent-testers/tests` |
