#!/bin/bash
# MediCare Standalone Debian Package (.deb) Builder
# Run this on your Linux partition (or inside WSL/Docker) to build the Linux installer.

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}====================================================${NC}"
echo -e "${CYAN}      MediCare Standalone Debian Package Builder    ${NC}"
echo -e "${CYAN}====================================================${NC}"

# Check JDK
if ! command -v java &> /dev/null; then
    echo -e "${RED}[ERROR] Java is not installed or not in your PATH.${NC}"
    echo -e "Please install OpenJDK 17 or higher (e.g. sudo apt install openjdk-17-jdk) to continue."
    exit 1
fi

# Check maven
MVN_CMD="mvn"
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}[WARNING] System 'mvn' not found. Checking local maven...${NC}"
    if [ -f "maven-dist/apache-maven-3.9.6/bin/mvn" ]; then
        MVN_CMD="maven-dist/apache-maven-3.9.6/bin/mvn"
        chmod +x "$MVN_CMD"
    else
        echo -e "${RED}[ERROR] Maven is not installed.${NC}"
        echo -e "Please install Maven (e.g. sudo apt install maven) to continue."
        exit 1
    fi
fi

# Check dpkg tools for packaging
if ! command -v dpkg &> /dev/null; then
    echo -e "${RED}[ERROR] dpkg tools are not available.${NC}"
    echo -e "This script must run on a Debian/Ubuntu-based Linux environment."
    exit 1
fi

# 1. Compile maven packages
echo -e "${GREEN}[1/3] Compiling package via Maven...${NC}"
$MVN_CMD clean package -DskipTests

JAR_PATH="target/medicare-system-1.0.0.jar"
JAVAFX_LIB="target/javafx-lib"

if [ ! -f "$JAR_PATH" ] || [ ! -d "$JAVAFX_LIB" ]; then
    echo -e "${RED}[ERROR] Maven build output is missing.${NC}"
    exit 1
fi

# 2. Setup jpackage input directory
echo -e "${GREEN}[2/3] Setting up packaging directories...${NC}"
JPACKAGE_INPUT="target/jpackage-input"
DIST_DIR="target/dist-linux"

rm -rf "$JPACKAGE_INPUT" "$DIST_DIR"
mkdir -p "$JPACKAGE_INPUT"

cp "$JAR_PATH" "$JPACKAGE_INPUT/medicare-system-1.0.0.jar"

# Check jpackage command
JAVA_CMD=$(which java)
JAVA_HOME_DIR=$(dirname $(dirname "$JAVA_CMD"))
JPACKAGE="$JAVA_HOME_DIR/bin/jpackage"

if [ ! -f "$JPACKAGE" ]; then
    # Try system PATH
    if command -v jpackage &> /dev/null; then
        JPACKAGE="jpackage"
    else
        echo -e "${RED}[ERROR] jpackage utility not found in JDK.${NC}"
        echo -e "Please ensure you have a complete JDK 17+ installed (not just JRE).${NC}"
        exit 1
    fi
fi

# 3. Package deb package using jpackage
echo -e "${GREEN}[3/3] Packaging into .deb package via jpackage...${NC}"
ICON_PATH="src/main/resources/static/assets/logo.png"

$JPACKAGE \
    --type deb \
    --name medicare \
    --app-version 1.0.0 \
    --input "$JPACKAGE_INPUT" \
    --main-jar medicare-system-1.0.0.jar \
    --main-class org.springframework.boot.loader.launch.JarLauncher \
    --dest "$DIST_DIR" \
    --icon "$ICON_PATH" \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base,javafx.media \
    --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" \
    --java-options "--add-opens=javafx.web/com.sun.webkit=ALL-UNNAMED" \
    --linux-shortcut \
    --linux-menu-group Office \
    --linux-app-category "office" \
    --linux-deb-maintainer "MediCare Maintainer <support@medicare.com>" \
    --description "MediCare Pharmacy Management System" \
    --about-url "https://github.com/Zohaib154/Pharmacy-Management-System"

DEB_FILE=$(find "$DIST_DIR" -name "*.deb" | head -n 1)

if [ -z "$DEB_FILE" ]; then
    echo -e "${RED}[ERROR] Debian package generation failed.${NC}"
    exit 1
fi

echo -e "${CYAN}====================================================${NC}"
echo -e "${GREEN}SUCCESS! Debian package created:${NC}"
echo -e "  - Location: $DEB_FILE"
echo -e "  - Install via: sudo dpkg -i $DEB_FILE"
echo -e "${CYAN}====================================================${NC}"
