# Security Policy

## Supported code

Security fixes target the latest code on `main` and, when practical, the most
recent Google Play build. Older builds are not maintained separately.

## Report a vulnerability privately

Do not open a public issue for a vulnerability or include exploit details,
credentials, personal data, or precise location information in a public post.

Use GitHub's **Report a vulnerability** button on the repository's Security tab:

<https://github.com/rupayon123/gta-free-stem-android/security/advisories/new>

If GitHub does not show that private form, use the
[public support page](https://gta-free-stem.vercel.app/support/) only to request
a private contact method. Do not include vulnerability details in that public
request.

Please include, when safe:

- the affected app version, Android version, and device type
- a concise description and impact
- reproducible steps or a minimal proof of concept
- whether credentials, local app data, or another person's information may be
  exposed
- any suggested mitigation

The maintainer will acknowledge and assess reports as capacity permits, avoid
unnecessary disclosure, and coordinate publication after a fix when possible.

## Security-sensitive project rules

- Never commit upload keys, keystores, signing passwords, API tokens, private
  certificates, Play Console exports, or real user data.
- Keep production network traffic on HTTPS and validate externally supplied
  opportunity data.
- Treat external registration, source, support, and map links as untrusted
  input.
- Preserve the account-free, no-ads, and no-third-party-analytics defaults
  unless a separately reviewed change explicitly updates privacy disclosures.

Incorrect public opportunity information is a content issue rather than a
software vulnerability. Report it in the
[opportunity-data repository](https://github.com/rupayon123/gta-free-stem-opportunities/issues/new/choose)
without including private information.
