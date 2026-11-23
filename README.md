# Food Score Light

A tool for the Light Phone III that looks up a product's health score
from its barcode.

## What it does

1. Type a product's barcode number (the digits printed under the black
   lines, not any other code on the package)
2. Food Score Light looks the product up via the [OpenFoodFacts](https://world.openfoodfacts.org/)
   API
3. See the product's Nutri-Score grade, a plain-language health
   rating, and the specific reasons behind it (sugar, saturated fat,
   salt, additives, processing level)
4. Favourite products to revisit them instantly later, without a new
   lookup

## Why manual entry

The SDK's third-party tool dependency allowlist doesn't currently
include CameraX/ML Kit, so camera-based barcode scanning isn't
possible yet (see [#160](https://github.com/lightphone/light-sdk/issues/160)
for context). Manual entry is the interim approach.

## Notable implementation details

- Uses a plain Compose `BasicTextField` instead of the SDK's
  `LightTextInputEditor`, to trigger the real system keyboard rather
  than the SDK's own embedded keyboard rendering (see
  [#161](https://github.com/lightphone/light-sdk/issues/161))
- Favourites are persisted locally via the SDK's built-in
  `lightContext.dataStore`, no backend required
- Health scoring leans on OpenFoodFacts' own published Nutri-Score
  system rather than a custom formula

## Data source

Product data comes from [OpenFoodFacts](https://world.openfoodfacts.org/),
a free, open, crowd-sourced database. Coverage is strongest for
European supermarket products and weaker for small/regional brands.
