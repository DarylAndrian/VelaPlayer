# Vela Unified Design Master Guide

## Main Color Theme Used

**Emerald Slate — preserved core theme**

![60% Main Color #09090B, 30% Secondary Color #1E2230, 10% Accent Color #10B981](../design-system/main-color-theme.svg)

| 60% — Main Color | 30% — Secondary Color | 10% — Accent Color |
| --- | --- | --- |
| **`#09090B`** | **`#1E2230`** | **`#10B981`** |
| Deep canvas | Slate cards and surfaces | Emerald actions and highlights |

These three anchors identify the core dark-native theme. The supporting Slate/Zinc shades below remain part of their respective groups. Light mode maps the same roles to **`#FAFAFA` / `#FFFFFF` / `#10B981`**.

**Maintained by:** Product Design + Frontend Engineering

**Version:** 3.0.0 — frontend-led design target

**Last updated:** 2026-11-18
**Identity:** Emerald Slate, original 60:30:10 palette preserved

This guide explains Vela's visual and interaction rules. [COMPONENTS.md](COMPONENTS.md) records current APIs and target component contracts. The [manifest](../design-system/vela.manifest.json) defines exact tokens and recipes for generation. The [audit](design-system/AUDIT.md) records implementation gaps; the [specification](design-system/SPECIFICATION.md) covers regeneration and staged adoption.

V3 consolidates the existing frontend's language. The palette below is preserved by user instruction. Other refinements are design targets; changing these files does not restyle production. Existing APIs and business enums remain authoritative until consumers migrate.

## 1. Design principles

- **Preserve Vela's identity:** deep canvas, slate/zinc surfaces, original emerald, typography, glass, Luna and recognizable card interactions.
- **Derive patterns from the app:** Product tiles, dashboard KPI tiles, Team Hub bento cards and reading panels have different purposes. Give each a named recipe.
- **Make structure predictable:** named tokens, a 4px grid, explicit states, shared density and accessibility behavior.
- **Make accents meaningful:** readable status labels, bounded highlights and progressive disclosure. Decoration cannot be the only status cue.
- **Refine dark mode locally:** improve text, boundaries, focus and elevation without replacing the palette or recoloring every card.

## 2. Preserved 60:30:10 composition

**60% backgrounds / 30% surfaces / 10% emerald accents** is a required visual hierarchy. Evaluate visual area and attention across a complete view. It is not a literal pixel-count rule for every component, nor a requirement to paint 10% of each card green.

### 60% — Dominant backgrounds

| Preserved value | Original role | Canonical v3 dark role |
| --- | --- | --- |
| `#09090b` | Deep page/dashboard canvas | `color/bg/canvas` |
| `#0f1117` | Base layout and inset areas | `color/bg/base` |
| `#181b23` | Supporting surface | `color/bg/subtle` |

### 30% — Secondary surfaces

| Preserved value / existing treatment | Purpose | Canonical v3 role |
| --- | --- | --- |
| `#1e2230` | Emerald Slate card surface | `color/bg/surface` in dark |
| `#252a3a` | Elevated/hover surface | `color/bg/hover` in dark |
| `#161922` | Sidebar foundation | `color/bg/sidebar` in dark |
| Zinc-900 `#18181b`, including 40% and 80% fills | Existing Product, Dashboard and Panel cards | `color/card/product`, `color/card/dashboard`, `color/card/panel` |
| Glass, soft borders and layered opacity | Product tiles, selected surfaces and shell layers | Named recipes; opacity applies to paint, not content |

Preserved Slate and existing Zinc recipes coexist. Do not globally convert Zinc cards to blue/slate cards. A transparent fill's apparent color depends on its documented backdrop.

### 10% — Original emerald accent

| Preserved value | Purpose |
| --- | --- |
| **`#10B981` / emerald-500** | Core brand, primary action fill, active indicators, accent bars and success identity |
| `#34D399` / emerald-400 | Supporting highlights and hover treatment |
| `#059669` / emerald-600 | Pressed treatment and supporting gradients |
| `rgba(16,185,129,0.35)` | Restrained emerald glow and expressive accents |

`color/brand/primary` and `color/action/primary/bg` resolve to **emerald-500 in both themes**. The build checks the original palette, dark role assignments and 60:30:10 shares against drift.

Primary actions use emerald-500 → emerald-400 on hover → emerald-600 when pressed, with deep `#09090b` labels throughout. The shared React Button now implements this pairing through semantic tokens; the old import path remains compatible. Inline screen-specific buttons and the native app still require migration. Do not keep unreadable white small text merely to match an old recipe.

Links and focus indicators are functional roles: light link/focus may use emerald-700; dark focus uses the core emerald. Contextual readable shades do not redefine the brand anchor. Light mode retains the frontend's white/Zinc hierarchy.

## 3. Tokens and semantic meaning

Use **primitive → semantic role → component recipe → product pattern**. Example: emerald-500 → `color/action/primary/bg` → Button → ConfirmDialog.

| Platform | Naming |
| --- | --- |
| JSON / Figma | `color/action/primary/bg` |
| CSS | `--vela-color-action-primary-bg` |
| Native JSON | Slash keys with resolved values; native adapters apply recipe opacity to background fills |

Use Zinc for generic neutrals while retaining the preserved custom Slate values. Avoid introducing unrelated gray/slate utility palettes. Decorative card borders stay soft; necessary input edges and focus indicators use separate contrast-checked roles.

| Business meaning | Target tone |
| --- | --- |
| Requested / idea | Neutral, with a label |
| In progress | Info |
| Planned / checkpoint | Warning |
| Completed / shipped / done | Success / emerald |
| Blocked / rejected | Danger, with explanation or next step |

Adapters standardize appearance without changing stored enums. `active` is domain-dependent. Priority, finance and workload retain their meaning. Product presets, saved custom accents and chart series are data colors, not replacement UI action colors. Preserve those accents with readable neutral labels.

The first implemented adapters cover workload capacity, milestone kinds, feature priorities and feature workflow. Light/Healthy/Stretched/Critical use success/info/warning/danger respectively; the original labels and stored values remain. Healthy now uses the shared sky info family, and Critical uses the shared red danger family. Release/Launch/Other keep success/info/neutral. Unknown values retain their supplied text with neutral colors instead of silently becoming healthy or completed.

Feature priority is an explicit domain exception: Critical/High/Medium/Low retain the existing red/orange/yellow/green order through readable priority roles. Low does not mean a completed workflow state. PRD-specific Workload BoardCard colors, numbered product tiers and Monday-linked task colors remain separate. New status shades are supporting semantic colors; Vela's preserved 60:30:10 anchors and original emerald primary remain unchanged.

Shared Badge is static wrapping metadata with a 12px label. DomainBadge applies the manifest's named adapter; native WorkloadSelect uses the same definitions and renders readable text in read-only mode. RoleBadge keeps existing elevated-role visibility and grants no permissions. Source definitions live in `domain.adapters`, generating a small `domain.json` for the runtime resolver. Consult COMPONENTS.md for actual migrated consumers and remaining inline presentations.

## 4. Card system derived from the frontend

**Products-style interactive cards are the leading inferred preference.** Evidence: [ProductCard](../src/screens/Product/ManageProducts.tsx), the explicitly named Products-style micro-interaction in [global CSS](../src/index.css), and reuse in [Dashboard KPI cards](../src/screens/Dashboard.tsx). This is an inference from reuse and code intent, not a confirmed personal ranking.

Preserve the rounded silhouette, soft border, subtle glass, product-colored icon well, compact title/summary and hover depth. The manifest defines these target recipes:

| Recipe | Appearance | Radius / padding | Interaction |
| --- | --- | --- | --- |
| Default | Slate card, 90% dark fill, 12px blur, white 6% dark edge | 12 / 20px | Static summary |
| Interactive | Same Slate surface; soft edge becomes accented on hover | 12 / 20px | 6px lift and 0.97 press scale |
| **Product** | White in light; zinc-900 at 40% in dark; 4px backdrop blur; accent hover edge | **16 / 20px** | Navigable tile; 6px lift, hover shadow, 0.97 press scale |
| Bento | Solid white/zinc-900, quiet border, no required blur | 16 / 24px | Grouped summaries; static by default |
| Dashboard | White/zinc-900 at 80% in dark; 2px corner accent | 12 / 16px | Lift only when actionable |
| Panel | Solid white/zinc-900; white 5% dark edge; optional header divider | 12 / 20px | Reading, tables, priorities and finance summaries |

Figma `Card` exposes Default/Interactive/Product/Bento. Shared web `Card` also exposes Dashboard and Panel recipes, with explicit `interactive` opt-in; it remains a native div. ProductCard and MetricCard supply the real primary actions. Existing React `Card` variants (`default`, `glass`, `bordered`) are compatibility mappings. Products, Dashboard KPIs, Team Hub containers/team cards/summary tiles and Dashboard priorities/finance panels now use shared recipes; People directory photo cards also use Product geometry, with natural-height text, separate native Open and sibling actions. Existing photos/illustration accents remain. Other cards remain migration work. See COMPONENTS.md and the implementation record.

### Glass and dark-mode refinement

Glass remains first-class; not every surface needs blur. Sidebar/header overlays can retain 12–24px blur. Card opacity applies to background paint only. Keep type and controls outside decorative opacity layers. Verify translucent recipes over known canvas/base backgrounds and use an opaque fallback over imagery or unknown backdrops.

Keep decorative borders restrained and necessary controls discernible. Avoid stacking glass into unreadable gray layers. Dark-mode improvements should clarify page/card/overlay hierarchy and muted text, preserving the original palette and existing Zinc cards.

### Hover and actions

Retain Product behavior: 500ms transition, `translateY(-6px)`, layered shadow and `scale(0.97)` on press. Use transforms on actionable cards with fine pointers; disable them for touch and reduced motion. Static panels remain still. Prevent overlap and focus clipping.

A card with edit/delete controls needs a primary link plus sibling actions. Do not nest buttons inside a button. Reveal secondary actions on keyboard focus as well as hover, and keep them available on touch. Saved product accents remain product-specific; the generator's emerald example represents Vela.

## 5. Typography, geometry and density

DM Sans is the body/UI font; Outfit is the display/metric font; Dancing Script is logo artwork only. Use Lucide for new UI. Saved Font Awesome product identifiers need a migration adapter, not deletion of stored icons. The landing page's isolated Newsreader editorial system remains an explicit exception.

| Role | Size / line height | Weight / font |
| --- | --- | --- |
| Display | 32 / 40 | 700 Outfit, brand/library cover |
| Page | 24 / 32 | 600 Outfit |
| Section | 18 / 24 | 600 DM Sans |
| Card title | 14 / 20 | 600 DM Sans |
| Body | 16 / 24 | 400 DM Sans |
| Body small | 14 / 20 | 400 DM Sans |
| Label | 14 / 20 | 500 DM Sans |
| Caption | 12 / 16 | 500 DM Sans |
| Metric | 28 / 36 | 700 Outfit, tabular digits |

These are target roles. Existing 10–11px metadata remains migration work. Keep mobile input text at least 16px, support OS text scaling and let content grow instead of clipping it to a specimen's dimensions.

| Geometry | Rule |
| --- | --- |
| Spacing | 4px base; 0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64 |
| Radius | Control 8; standard card/panel 12; product/bento/dialog 16; pill 999 |
| Controls | sm=32, md=40, lg=48px minimum visual height; touch regions ≥48 |
| Density | Compact row ≥40 / padding 16; comfortable row ≥48 / padding 24; Product/Panel padding 20 is a named recipe |
| Layout | 4/8/12 columns; gutters 16/24/24; content maximum 1440 |
| Dialog | Default max width 480, wide max width 960, full for large editors. Viewport gutter 16 and scrollable body. |
| Icons | 16/20/24; glyph size independent of accessible action target |

### Screen composition and scroll ownership

Shared PageHeader uses the page/section roles above and a wrapping action slot. PageToolbar groups persistent labelled controls and counts with native Tab navigation. SearchField includes a named clear action that returns focus to the input. Products, Team Hub, People and Learning use shared search controls. Dashboard, administration and private web tools also use shared actions and form patterns. Existing queries, options, callbacks and access checks remain screen-owned.

Choose one scroll strategy deliberately. PageContent `scroll=page` owns document-style scrolling. A bounded `scroll=contained` workspace uses PageFill and named keyboard-scrollable ScrollArea regions, keeping its header/filter controls outside the results scroll. Overview and Milestones enable adaptive fallback: below the manifest's 768px width or 720px height thresholds, full-height containers grow and the page owns scrolling. Timeline keeps explicit containment. Do not add viewport heights or independent scrollbars to every nested wrapper.

Use DataRow for flexible non-table content with leading context, body and sibling actions. Required titles and notes wrap; actions move beneath the body below 640px. Keep native table semantics for actual tables. These code compositions reuse the existing design-system primitives; this slice does not add corresponding Figma sets. APIs and current adoption are recorded in COMPONENTS.md.

## 6. States, accessibility and motion

Define default/hover/pressed/focus/disabled/loading where applicable. Inputs also need error/read-only. Static Card hover/focus slots are neutral equivalents for matrix consistency; a specimen does not make a static panel focusable.

Target WCAG 2.2 AA: 4.5:1 normal text, 3:1 large text and applicable control indicators. Vela's 48px touch policy is a chosen convention, not the AA minimum. Check actual foreground/background pairs, including known glass composites, rather than changing the brand globally.

Use a 2px focus ring with 2px offset. Status needs a label/icon beyond color. Persist field labels and associate hints/errors. Dialogs need title association, managed focus/trap/return and safe destructive activation. Loading preserves accessible names, exposes busy state and prevents duplicate actions. Read-only content remains readable/selectable. Member add/edit/detail/removal and the active image crop/size dialogs now follow these shared rules. Upload controls remain outside the member form; a nested Escape closes only the top dialog, and uploads stay modal until completion.

Routine transitions use 120/180/240ms; Product cards retain 500ms. Aurora, shimmer, glow and Luna remain available for meaningful activity and brand expression. Avoid animating every panel indefinitely. Reduced motion removes decorative loops and transforms; changing a duration variable alone does not stop old CSS/JS loops. Check native accessibility preferences too.

## 7. Ownership and adoption

Product Design and Frontend Engineering share ownership. A component needs documented states/properties, semantic bindings, responsive behavior, accessibility evidence and a code mapping before adoption. Use semver and keep deprecated aliases for one minor release with a replacement/removal version. Exceptions need path, owner, reason and expiry.

The palette/composition are preserved requirements; other refinements are documented targets. Production APIs remain until component-by-component migration. Manifest paths are mapping candidates, not registered Code Connect mappings. Updating a guide or running the loader does not migrate the app automatically.

Adopted files enroll in `governance.enforcedPaths`. The focused ESLint policy rejects copied native controls/manual modal roles and literal Button palette overrides in that set; semantic compositions and native form structure remain available. `design-system:check` verifies generated drift, this policy and its regression tests. CI is configured to run it and the isolated shared UI behavior suite. Keep remaining legacy screens visible in [ROLLOUT.md](design-system/ROLLOUT.md) and enroll each completed migration after verification.

Product create/edit now use the shared Dialog, including visible errors and busy/focus guards. The nested icon picker uses the same overlay contract, preserving saved Font Awesome identifiers and immediate edit-icon writes. Required names and draft colors still save through their original product callbacks. These are code compositions of existing foundations, not additional generated Figma sets.

## 8. Generation and CSS reference

Edit this guide for rationale, COMPONENTS.md for APIs/mappings and the manifest for exact values/recipes. Then run:

```sh
npm run design-system:build
npm run design-system:check
npm run design-system:preview-check
```

Review the [preview](../design-system/preview.html), import the [Figma plugin manifest](../tools/figma-vela-design-system/manifest.json), and run **Vela Design System v3**. Inspect the actual Figma output before publication, then migrate frontend consumers separately.

The web entry now imports generated `design-system/tokens.css` once, after the existing global stylesheet. It exports semantic variables and `.vela-card-product`, `.vela-card-bento` and other recipe classes. Shared controls consume these roles; remaining inline screen styling is being migrated incrementally. Previous documentation's short CSS names are historical references. See [IMPLEMENTATION.md](design-system/IMPLEMENTATION.md) for actual adoption status.

```css
.vela-primary-action {
  background: var(--vela-color-action-primary-bg); /* original emerald-500 */
  color: var(--vela-color-action-primary-fg);       /* deep readable text */
  min-height: var(--vela-size-control-md);
  border-radius: var(--vela-radius-control);
}
```

The build validates palette/composition preservation, shape, references, matrices, generated drift and contrast over known backdrops. Local API tests do not prove real Figma rendering or runtime accessibility.
