# Google Play Data safety declaration

Use these answers for package `com.photosoap`. They mirror the shipping optional analytics behavior and should be reviewed again whenever the metrics payload changes.

## Data collected

Collection begins only after the user explicitly opts in. The app remains fully functional when the user declines.

| Google Play data type | Collected | Shared | Purpose | Required |
| --- | --- | --- | --- | --- |
| App activity → App interactions | Yes | No | Analytics | Optional |
| Device or other IDs | Yes | No | Analytics | Optional |

App interactions are daily totals for items reviewed, kept, and deleted plus estimated bytes freed. The identifier is a random UUID generated for this installation of PhotoSoap. It is not an advertising ID, hardware ID, account ID, email address, or name.

The payload also includes the app version, build number, platform, submission date, and an install-registration flag. It is sent over HTTPS to PhotoSoap's Supabase service provider.

## Data not collected

PhotoSoap does not transmit photos, videos, filenames, media identifiers, location data, contacts, advertising data, crash logs, financial information, or messages. Access to the media library is processed on-device and therefore is not declared as collection in the Data safety form.

## Console answers

- Does the app collect or share user data? **Yes — collects data only when the user opts in.**
- Is all collected data encrypted in transit? **Yes.**
- Is collection optional? **Yes, for both declared data types.**
- Is data shared with third parties? **No.** Supabase processes the data as PhotoSoap's service provider.
- Is the data processed ephemerally? **No.** Submitted aggregate records may remain in community totals.
- Does the app create accounts? **No.**
- Does the app use data for advertising, marketing, personalization, or tracking? **No.**

The privacy-policy URL for the Play listing is `https://dermoha.github.io/PhotoSoap/privacy.html`.
