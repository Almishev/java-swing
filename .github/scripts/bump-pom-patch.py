#!/usr/bin/env python3
"""Bump semver patch in pom.xml project <version> (next to artifactId swing-system)."""
from __future__ import annotations

import re
import sys
from pathlib import Path


PROJECT_VERSION_PATTERN = re.compile(
    r'(<artifactId>swing-system</artifactId>\s*)'
    r'(<version>)([^<\s]+)(</version>)',
    re.MULTILINE,
)


def bump_patch(raw_version: str) -> str:
    suffix = ''
    core = raw_version.strip()
    if core.endswith('-SNAPSHOT'):
        suffix = '-SNAPSHOT'
        core = core[: -len('-SNAPSHOT')]

    parts = core.split('.') if core else ['0']
    while len(parts) < 3:
        parts.append('0')
    major, minor, patch = parts[0], parts[1], parts[2]
    try:
        patch = str(int(patch) + 1)
    except ValueError as exc:
        raise SystemExit(f"Cannot bump non-numeric patch in version: {raw_version!r}") from exc

    semver = '.'.join([major, minor, patch])
    if len(parts) > 3:
        semver += '.' + '.'.join(parts[3:])
    return semver + suffix


def main() -> None:
    pom_path = Path('pom.xml')
    pom = pom_path.read_text(encoding='utf-8')
    match = PROJECT_VERSION_PATTERN.search(pom)
    if not match:
        sys.exit(
            'Could not find swing-system artifact <version>...</version> block in pom.xml'
        )

    current = match.group(3).strip()
    new_ver = bump_patch(current)
    replaced = PROJECT_VERSION_PATTERN.sub(
        rf'\g<1>\g<2>{new_ver}\g<4>',
        pom,
        count=1,
    )
    pom_path.write_text(replaced, encoding='utf-8')
    print(new_ver)


if __name__ == '__main__':
    main()
