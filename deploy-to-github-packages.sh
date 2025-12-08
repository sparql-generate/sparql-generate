#!/bin/bash

# Script to deploy to GitHub Packages from local machine
# Usage: ./deploy-to-github-packages.sh [module-name]
# Example: ./deploy-to-github-packages.sh sparql-generate-jena

set -e

GITHUB_REPO="ACCURIDS/sparql-generate"
GITHUB_PACKAGES_URL="https://maven.pkg.github.com/${GITHUB_REPO}"

# Check if Maven settings.xml exists and has GitHub configuration
check_maven_settings() {
    if [ ! -f ~/.m2/settings.xml ]; then
        echo "❌ Error: ~/.m2/settings.xml not found!"
        echo ""
        echo "Please create ~/.m2/settings.xml with GitHub Packages authentication."
        echo "See DEPLOY_TO_GITHUB_PACKAGES.md for instructions."
        exit 1
    fi
    
    if ! grep -q "<id>github</id>" ~/.m2/settings.xml; then
        echo "❌ Error: GitHub server configuration not found in ~/.m2/settings.xml!"
        echo ""
        echo "Please add GitHub Packages server configuration."
        echo "See DEPLOY_TO_GITHUB_PACKAGES.md for instructions."
        exit 1
    fi
    
    echo "✅ Maven settings.xml found with GitHub configuration"
}

# Deploy function
deploy_module() {
    local module=$1
    local pom_file
    
    if [ -z "$module" ]; then
        # Deploy parent (all modules)
        pom_file="sparql-generate-parent/pom.xml"
        echo "📦 Deploying all modules from parent POM..."
    else
        # Deploy specific module
        pom_file="${module}/pom.xml"
        if [ ! -f "$pom_file" ]; then
            echo "❌ Error: Module '$module' not found!"
            echo "Available modules:"
            ls -d sparql-generate-*/ | sed 's|/$||' | sed 's|^|  - |'
            exit 1
        fi
        echo "📦 Deploying module: $module"
    fi
    
    echo "🚀 Starting deployment to GitHub Packages..."
    echo "   Repository: $GITHUB_PACKAGES_URL"
    echo ""
    
    mvn clean deploy -P docs -DskipTests \
        -DaltDeploymentRepository=github::${GITHUB_PACKAGES_URL} \
        --file "$pom_file"
    
    echo ""
    echo "✅ Deployment completed!"
    echo "📋 Check your packages at: https://github.com/${GITHUB_REPO}/packages"
}

# Main
echo "🔧 GitHub Packages Deployment Script"
echo "===================================="
echo ""

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: $0 [module-name]"
    echo ""
    echo "Examples:"
    echo "  $0                          # Deploy all modules"
    echo "  $0 sparql-generate-jena     # Deploy specific module"
    echo ""
    exit 0
fi

check_maven_settings
echo ""

deploy_module "$1"
