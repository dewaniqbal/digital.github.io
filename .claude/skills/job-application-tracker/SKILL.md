---
name: job-application-tracker
description: Create, update, clean, and summarize CSV job application trackers with statuses, next actions, contacts, follow-up dates, and notes.
---

# Job Application Tracker

Use this skill when the user wants to create, update, organize, or summarize a job application tracker.

## CSV schema

Use these columns by default:

```csv
company,role,status,source,url,applied_date,next_action,next_action_date,contact,notes,last_updated
```

## Status values

Recommended statuses:

- Saved
- Applied
- Recruiter screen
- Interviewing
- Take-home
- Final round
- Offer
- Rejected
- Withdrawn
- Closed

## Workflow

1. If no tracker exists, create one with the default schema.
2. Add one row per company + role.
3. Update existing rows instead of duplicating when company + role match.
4. Suggest a next action if missing.
5. Suggest a next action date if missing.
6. Preserve old notes unless the user asks to replace them.
7. Summarize upcoming actions.

## Output format

When creating or updating a tracker, provide:

```markdown
## Tracker update

- Added/updated: [Company] — [Role]
- Status: [Status]
- Next action: [Next action]
- Next action date: [Date]

## CSV row

```csv
company,role,status,source,url,applied_date,next_action,next_action_date,contact,notes,last_updated
...
```
```

## Privacy

Do not store unnecessary sensitive data in the tracker. Avoid home addresses, IDs, private references, or personal health/family information.
