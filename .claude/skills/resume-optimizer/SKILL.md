---
name: resume-optimizer
description: Tailor a resume or CV to a specific job description with ATS keyword audit, truthful bullet rewrites, summary rewrite, and gap notes.
---

# Resume Optimizer

Use this skill when the user asks to improve, tailor, rewrite, or optimize a resume/CV for a specific role or job description.

## Rules

- Do not invent experience, metrics, tools, degrees, employers, dates, certifications, or outcomes.
- Use job-description keywords only when they truthfully fit the candidate.
- Preserve the candidate's actual career history.
- Mark missing proof with placeholders such as `[add metric if true]`.
- Keep the result easy to copy into a resume document.

## Workflow

1. Read the resume/CV and job description.
2. Identify the target role, company, must-have skills, nice-to-have skills, tools, and business outcomes.
3. Create an ATS keyword audit:
   - matched terms
   - missing terms
   - unsupported terms
   - alternative phrasing
4. Create a role-match matrix.
5. Rewrite the resume headline and summary.
6. Rewrite the most relevant bullets using action + scope + tool/method + result.
7. Suggest skills section changes.
8. List facts the user must verify before submitting.

## Output format

```markdown
# Resume Optimization for [Role] at [Company]

## ATS Keyword Audit

## Role Match Matrix

## Recommended Headline

## Rewritten Summary

## Rewritten Experience Bullets

## Skills Section Suggestions

## Gaps and Honest Bridges

## Verification Checklist
```

## Bullet rewrite pattern

```text
Action verb + scope/context + tool/method + measurable result or business value.
```

Example:

```text
Built Tableau dashboards for sales and operations teams, helping stakeholders monitor weekly revenue, pipeline, and customer trends.
```
