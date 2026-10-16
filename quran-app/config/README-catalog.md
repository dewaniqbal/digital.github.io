# Catalog notes

`core/src/main/resources/catalog.json` is the bundled fallback catalog. A newer catalog can be
served from the URL in `config/app-config.template.properties` (`CATALOG_URL`) without rebuilding.

## Requested reciters awaiting a verified audio source
These were requested by the product owner and are listed with **no recitations**. The app shows
them as "Audio source not yet verified" and disables Play. Do not add URLs until a provider that
permits streaming (and ideally offline use) is identified and its terms are read.

Qari Mahmudul Hasan, Saleh Ahmad Takrim, Hafez Abu Rayhan, Nuruddin Muhammad Zakaria,
Hafez Nazmus Sakib, Saifur Rahman Taqi, Tarikul Islam, Helal Uddin, Yakub Hossain Taj,
Saud Al Jumaa, Fazle Rabbi Adil.

Arabic names, countries and recitation styles are intentionally left empty until verified.
To enable one: add a `source` (with `attribution`, `licenseSummary`, `allowedHosts`,
`streamingAllowed`) and a `recitation` with `urlTemplate` or `urls`, then run the catalog tests.
