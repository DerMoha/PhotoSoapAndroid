# Product polish backlog

Working notes from simulator and product-review sessions. Capture observations here first; implementation can be grouped into a later polish pass.

## High priority

### Rework onboarding experience

- [ ] Replace the current onboarding visual treatment with a more premium, intentional first-run experience.
  - Feedback: the current screen feels cheap and does not establish the quality bar for PhotoSoap.
  - Scope: review the Android and iOS onboarding screens together so they feel like one product.
  - Revisit: visual hierarchy, brand expression, typography, benefit presentation, CTA treatment, spacing, and subtle motion.


## Android beta pass (2026-10-03)

Android onboarding now uses theme-aware contrast, readable stacked benefit rows,
a library identity mark, and explicit deletion-safety copy. Visual/device sign-off
and comparison with iOS remain outstanding; the cross-platform redesign item above
is intentionally not marked complete without that review.

## Android simulator validation (2026-10-04)

The updated onboarding, review, settings, stats, achievements and privacy views were inspected in the Android emulator. German, dark mode and 200% text were checked. Additional switch labeling, one-line navigation labels and compact-height review/gesture fixes were implemented. Cross-platform onboarding comparison and physical-device sign-off remain separate.
