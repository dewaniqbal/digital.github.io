---
name: job-seeker-helper
description: End-to-end job search application workflow for resumes, CVs, cover letters, recruiter messages, interview prep, follow-ups, and job trackers.
---

# Job Seeker Helper

Use this skill when the user wants help applying to a job, improving a resume/CV for a specific job description, writing a cover letter, preparing for interviews, contacting recruiters, writing follow-ups, or tracking job applications.

## Operating rules

1. Preserve truth. Do not invent employment history, titles, dates, degrees, certifications, tools, metrics, projects, publications, patents, awards, citizenship, visa status, security clearance, or outcomes.
2. If evidence is missing, use a clearly labeled placeholder such as `[add metric if true]` or ask for the missing fact.
3. Keep the candidate's voice. Improve clarity and relevance without making the application sound fake or over-polished.
4. Separate facts from assumptions. Mark assumptions clearly.
5. Prefer useful outputs the user can send today.
6. Avoid including sensitive personal data unless the user provided it and it is relevant.

Read `resources/ethics.md` when privacy, truthfulness, protected characteristics, or sensitive data appears in the task.

## Default workflow

When the user gives a resume/CV and job description:

1. Identify the role, company, seniority, location, and must-have requirements.
2. Create an ATS keyword audit.
3. Build a role-match matrix that maps job requirements to real candidate evidence.
4. Rewrite the resume summary and selected bullets.
5. Draft a tailored cover letter.
6. Draft a recruiter or hiring manager message when useful.
7. Build interview prep.
8. Create follow-up emails.
9. Add or update a job tracker row.
10. Combine outputs into an application packet if requested.

For details, read `resources/workflow.md`.

## Output files

Use these filenames unless the user requests something else:

- `ats_keyword_audit.md`
- `tailored_resume.md`
- `cover_letter.md`
- `recruiter_message.md`
- `interview_prep_pack.md`
- `follow_up_email.md`
- `job_tracker.csv`
- `application_packet.md`

## Helper scripts

Use scripts only when files are available locally and code execution is appropriate.

- `scripts/keyword_audit.py`: compare resume text to job description text.
- `scripts/job_tracker.py`: create or update a CSV application tracker.
- `scripts/packet_builder.py`: combine Markdown files into one application packet.
- `scripts/validate_skill.py`: validate this skill package.

Examples:

```bash
python .claude/skills/job-seeker-helper/scripts/keyword_audit.py --resume resume.md --job jd.md --output ats_keyword_audit.md
python .claude/skills/job-seeker-helper/scripts/job_tracker.py --file job_tracker.csv --company "ExampleCo" --role "Data Analyst" --status "Applied"
python .claude/skills/job-seeker-helper/scripts/packet_builder.py --output application_packet.md ats_keyword_audit.md tailored_resume.md cover_letter.md interview_prep_pack.md
```

## Resource routing

- Resume/CV tailoring: read `resources/resume_tailoring.md`.
- Cover letter: read `resources/cover_letter.md`.
- Recruiter or hiring manager message: read `resources/recruiter_message.md`.
- Interview prep: read `resources/interview_prep.md`.
- Follow-ups: read `resources/followups.md`.
- Job tracker: read `resources/tracker.md`.
- Document formatting: read `resources/document_output.md`.
- Ethical boundaries: read `resources/ethics.md`.

## Response style

Be practical, clear, and direct. Use simple language. Give the user the finished draft first, then a short explanation of what changed and what they should verify.
