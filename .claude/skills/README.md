# Project skills

Claude Code loads every folder here that holds a `SKILL.md` (invoke with `/<name>`).
55 skills, all copied from public GitHub repos on 2026-11-11.

## Video editing and video app development
| Skills | Source | License |
|---|---|---|
| remotion-best-practices, remotion-create, remotion-captions, remotion-docs, remotion-interactivity, remotion-license, remotion-maps, remotion-markup, remotion-multimedia, remotion-render, remotion-saas, remotion-studio, remotion-upgrade | [remotion-dev/skills](https://github.com/remotion-dev/skills) (official) | No LICENSE file in the repo; the skills come from the Remotion monorepo, whose Remotion License requires a paid license for larger companies. See `/remotion-license` |
| video, video-edit, video-create, video-caption, video-audio, video-enhance, video-enhance-audio, video-export, video-transcode, video-shorts, video-promo, video-image, video-screenshot, video-analyze, video-generate, video-download | [agricidaniel/claude-video](https://github.com/agricidaniel/claude-video) | MIT |
| ffmpeg, moviepy, video-design, playwright-recording, youtube-stats | [digitalsamba/claude-code-video-toolkit](https://github.com/digitalsamba/claude-code-video-toolkit) | MIT |
| heygen-video, heygen-avatar, heygen-translate | [heygen-com/skills](https://github.com/heygen-com/skills) (official, needs a HeyGen account) | MIT |
| video-downloader | [ComposioHQ/awesome-claude-skills](https://github.com/ComposioHQ/awesome-claude-skills) | No LICENSE file found in the repo |
| youtube-transcript | [michalparkola/tapestry-skills-for-claude-code](https://github.com/michalparkola/tapestry-skills-for-claude-code) | MIT |
| frontend-design, webapp-testing, web-artifacts-builder | [anthropics/skills](https://github.com/anthropics/skills) | Apache-2.0 |

## Job research and applications
| Skills | Source | License |
|---|---|---|
| job-search-setup (was `setup`), job-search, network-scan, tailor-resume, cover-letter, job-apply (was `apply`), jobsearch-telegram, plus `_proficiently-shared/` reference files | [proficientlyjobs/proficiently-claude-skills](https://github.com/proficientlyjobs/proficiently-claude-skills) | MIT according to its plugin.json (no LICENSE file in the repo) |
| job-seeker-helper, resume-optimizer, interview-coach, job-application-tracker, cover-letter-coach (was `cover-letter`) | [Faizee-Asad/job-seeker-claude-skills](https://github.com/Faizee-Asad/job-seeker-claude-skills) | MIT |
| tailored-resume-generator | [ComposioHQ/awesome-claude-skills](https://github.com/ComposioHQ/awesome-claude-skills) | No LICENSE file found in the repo |

Local changes: renamed skills to avoid clashes (listed above), and pointed the Proficiently skills'
`shared/...` references to `.claude/skills/_proficiently-shared/...`.
Most video skills need `ffmpeg` (and Node.js for Remotion) installed.
