#!/bin/bash
# Reads version from version.properties and updates all references across the monorepo.
set -e
VERSION=$(grep '^version=' version.properties | cut -d= -f2)
echo "Updating to version $VERSION"

# Gradle lib (reads at build time, no change needed)

# npm package
cd bff-types
npm version "$VERSION" --no-git-tag-version --allow-same-version 2>/dev/null
cd ..

# Docs site
sed -i '' "s|bff-spring-lib:[0-9]*\.[0-9]*\.[0-9]*|bff-spring-lib:${VERSION}|g" docs/index.html
sed -i '' "s|<version>[0-9]*\.[0-9]*\.[0-9]*</version>|<version>${VERSION}</version>|g" docs/index.html
sed -i '' "s|<small>v[0-9]*\.[0-9]*\.[0-9]*</small>|<small>v${VERSION}</small>|g" docs/index.html

# Root README
sed -i '' "s|bff-spring-lib:[0-9]*\.[0-9]*\.[0-9]*|bff-spring-lib:${VERSION}|g" README.md

# Spring lib README
sed -i '' "s|bff-spring-lib:[0-9]*\.[0-9]*\.[0-9]*|bff-spring-lib:${VERSION}|g" bff-spring-lib/README.md
sed -i '' "s|<version>[0-9]*\.[0-9]*\.[0-9]*</version>|<version>${VERSION}</version>|g" bff-spring-lib/README.md

echo "Done. All references updated to $VERSION"
