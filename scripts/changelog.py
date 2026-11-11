#!/usr/bin/env python3
"""
Builds the YouTube Music part of the Morphe patches changelog for a patches version.

Stable version: its own section (it already lists everything since the previous stable).
Dev version: new features of the version itself, plus those of the previous dev when that one was not
released (RELEASED), bug fixes and improvements of the version itself only. When the stable version before
it was not released either (the first dev build after a skipped stable), the full section of that stable
version follows: its new features after those of the dev, its fixes after those of the dev.

Each group is a title followed by its changes, without version headings:

    ✨ New Features
    • ...
    🌟 Mod Features     (from MOD_FEATURES)
    • ...
    🐛 Bug Fixes
    • ...              (Morphe fixes, then the fixes of the mod from MOD_FIXES)

The in-app dialog shows the titles bold with a thin rule under them. The release notes
use "## <title>" headings (GitHub draws the same kind of rule) and add the files table.

Newer versions come first; within a version the order of the Morphe changelog is kept.

RELEASED: patches versions that already have a release of the mod, one per line or separated by spaces.

MOD_FIXES: "## <patches version>" headings with "* " items, listed after the Morphe fixes of that version.

EXTRA_FEATURES: same format, listed after the Morphe new features of that version. Only for releases that
skip versions (for example the first dev build after a stable one): important features of the skipped
versions that the selected sections do not list. Not a general rule; most versions have no section.

The Markdown file is the release notes, the text file is the changelog of the in-app update dialog.

Usage: changelog.py CHANGELOG.md VERSION OUT_MARKDOWN OUT_TEXT [--mod-features FILE] [--mod-fixes FILE]
                    [--extra-features FILE] [--released FILE]
"""
import argparse
import re

SECTION = re.compile(r"^##\s+\[([^\]]+)\]")
OTHER_APPS = ("youtube", "reddit")
SKIPPED_GROUPS = ("app support",)
DEV_FEATURE_SECTIONS = 2  # Dev changelogs: at most the version itself and the unreleased dev before it.

# Change types in the order they are shown. Older changelogs have titles without emoji.
GROUPS = (
    ("feature", "✨ New Features"),
    ("fix", "🐛 Bug Fixes"),
    ("improvement", "🔧 Improvements"),
)
FIX_GROUP = 1
MOD_FEATURES_TITLE = "🌟 Mod Features"


def clean(text):
    text = re.sub(r"\s*\(\[[0-9a-f]{7,40}\]\([^)]*\)\)", "", text)       # (commit)
    text = re.sub(r"\s*\(\[#\d+\]\([^)]*\)\)", "", text)                  # (#PR)
    text = re.sub(r",?\s*closes\s+.*$", "", text, flags=re.I)              # closes #123
    text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)                   # [text](url)
    return re.sub(r"\s{2,}", " ", text).strip()


def strip_app(item):
    """'**YouTube Music - Scope:** text' -> '**Scope:** text', '**YouTube Music:** text' -> 'text'."""
    # Shared scopes such as "YouTube / YouTube Music - Scope" are shortened the same way
    apps = r"(?:YouTube\s*/\s*)?YouTube Music(?:\s*/\s*[^*:-]+?)?"
    item = re.sub(rf"^\*\*{apps}:\*\*\s*", "", item)
    return re.sub(rf"^\*\*{apps} - ", "**", item)


def relevant(item):
    m = re.match(r"\*\*([^*]+):\*\*", item)
    if not m:
        return True  # General change.
    scope = m.group(1).lower()
    # "YouTube / YouTube Music - Scope": the apps before " - "; YouTube Music among them keeps the change
    apps = [app.strip() for app in scope.split(" - ")[0].split("/")]
    if "youtube music" in apps:
        return True
    return not any(app in OTHER_APPS for app in apps)


def group_index(title):
    title = title.lower()
    return next((i for i, (word, _) in enumerate(GROUPS) if word in title), len(GROUPS) - 1)


def parse(path):
    """[{version, groups: {group index: [item]}}], newest first, items in the order of Morphe."""
    sections, current, group = [], None, None
    for line in open(path, encoding="utf-8"):
        line = line.rstrip("\n")
        m = SECTION.match(line)
        if m:
            current = {"version": m.group(1), "groups": {}}
            sections.append(current)
            group = None
        elif current is not None and line.startswith("### "):
            title = line[4:].strip()
            group = None if any(word in title.lower() for word in SKIPPED_GROUPS) else group_index(title)
        elif current is not None and group is not None and line.startswith("* "):
            item = clean(line[2:])
            if relevant(item):
                current["groups"].setdefault(group, []).append(strip_app(item))
    return sections


def select(sections, version, released=()):
    """(sections of the new features, sections of the fixes and improvements), newest first."""
    index = next((i for i, s in enumerate(sections) if s["version"] == version), None)
    if index is None:
        raise SystemExit(f"Version {version} not found in changelog")
    if "-dev" not in version:
        return [sections[index]], [sections[index]]
    selected, stable = [], None
    for section in sections[index:]:
        if "-dev" not in section["version"]:
            # A stable version that was not released: its full changelog follows
            if section["version"] not in released and section["groups"]:
                stable = section
            break
        # A dev that was already released listed its own features
        if section["version"] != version and section["version"] in released:
            break
        if section["groups"]:
            selected.append(section)
    features = selected[:DEV_FEATURE_SECTIONS]
    fixes = [s for s in features if s["version"] == version]
    if stable:
        features.append(stable)
        fixes.append(stable)
    return features, fixes


def read_mod_features(path):
    items = []
    for line in open(path, encoding="utf-8"):
        line = line.strip()
        if line.startswith(("* ", "- ")):
            items.append(line[2:].strip())
    return items


def read_version_items(path, version):
    items, current = [], None
    for line in open(path, encoding="utf-8"):
        line = line.strip()
        if line.startswith("## "):
            current = line[3:].strip().lstrip("v")
        elif current == version and line.startswith(("* ", "- ")):
            items.append(line[2:].strip())
    return items


def read_released(path):
    return {v.lstrip("v") for v in open(path, encoding="utf-8").read().split()}


def plain(item):
    return item.replace("**", "").replace("`", "")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("changelog")
    parser.add_argument("version")
    parser.add_argument("out_md")
    parser.add_argument("out_txt")
    parser.add_argument("--mod-features")
    parser.add_argument("--mod-fixes")
    parser.add_argument("--extra-features")
    parser.add_argument("--released")
    args = parser.parse_args()

    version = args.version.lstrip("v")
    released = read_released(args.released) if args.released else set()
    feature_sections, fix_sections = select(parse(args.changelog), version, released)
    mod_fixes = read_version_items(args.mod_fixes, version) if args.mod_fixes else []
    extra_features = read_version_items(args.extra_features, version) if args.extra_features else []
    mod_features = read_mod_features(args.mod_features) if args.mod_features else []

    groups = []
    for index, (_, title) in enumerate(GROUPS):
        # New features: all selected versions. Fixes and improvements: the version itself (and a skipped stable).
        sections = feature_sections if index == 0 else fix_sections
        items = []
        for item in (item for s in sections if index in s["groups"] for item in s["groups"][index]):
            if item not in items:  # A change listed again in a later version
                items.append(item)
        if index == 0:
            items += extra_features
        if index == FIX_GROUP:
            items += mod_fixes
        if items:
            groups.append((title, items))
        # The features of the mod follow the new features of Morphe.
        if index == 0 and mod_features:
            groups.append((MOD_FEATURES_TITLE, mod_features))

    md, txt = [], []
    for title, items in groups:
        # Markdown: "##" headings get GitHub's thin rule under them.
        md += [f"## {title}", ""]
        md += [f"* {item}" for item in items]
        md.append("")
        # The app draws a thin rule under each title.
        txt += [title]
        txt += [f"• {plain(item)}" for item in items]
        txt.append("")

    open(args.out_md, "w", encoding="utf-8").write("\n".join(md).strip() + "\n")
    open(args.out_txt, "w", encoding="utf-8").write("\n".join(txt).strip() + "\n")


if __name__ == "__main__":
    main()
