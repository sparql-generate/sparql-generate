# Deploying to GitHub Packages from Local Machine

This guide explains how to deploy Maven packages to GitHub Packages from your local machine.

## Prerequisites

1. **GitHub Personal Access Token (PAT)**
   - Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Click "Generate new token (classic)"
   - Give it a name (e.g., "Maven GitHub Packages")
   - Select scopes: `write:packages` and `read:packages`
   - Generate and **copy the token** (you won't see it again!)

2. **Maven installed** (version 3.5+)

## Setup Maven Settings

Create or edit `~/.m2/settings.xml` with the following content:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

**Replace:**
- `YOUR_GITHUB_USERNAME` with your GitHub username (e.g., `ACCURIDS`)
- `YOUR_PERSONAL_ACCESS_TOKEN` with the token you generated

**Security Note:** Make sure `~/.m2/settings.xml` has proper permissions:
```bash
chmod 600 ~/.m2/settings.xml
```

## Testing Deployment

### Option 1: Use the Deployment Script (Recommended)

The easiest way to deploy is using the provided script:

```bash
# Deploy all modules
./deploy-to-github-packages.sh

# Deploy a specific module
./deploy-to-github-packages.sh sparql-generate-jena
```

The script will:
- Check that your Maven settings.xml is configured correctly
- Deploy to GitHub Packages using the correct repository URL
- Show you where to verify your packages

### Option 2: Manual Deployment - All Modules

Deploy all modules from the parent POM (run from project root):

```bash
mvn clean deploy -P docs -DskipTests \
  -DaltDeploymentRepository=github::https://maven.pkg.github.com/ACCURIDS/sparql-generate \
  --file sparql-generate-parent/pom.xml
```

### Option 3: Manual Deployment - Specific Module

Deploy a specific module (run from project root):

```bash
mvn clean deploy -P docs -DskipTests \
  -DaltDeploymentRepository=github::https://maven.pkg.github.com/ACCURIDS/sparql-generate \
  --file sparql-generate-jena/pom.xml
```

**Note:** Replace `sparql-generate-jena` with the name of the module you want to deploy.

## Verify Deployment

1. Go to your GitHub repository: `https://github.com/ACCURIDS/sparql-generate`
2. Click on "Packages" (on the right side of the repository page)
3. You should see your published packages

## Using the Published Packages

To use packages from GitHub Packages in another project, add this to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/ACCURIDS/sparql-generate</url>
  </repository>
</repositories>
```

And in `~/.m2/settings.xml`, add the same server configuration for authentication when downloading.

## Troubleshooting

### Error: 401 Unauthorized

This is an authentication error. Follow these steps:

1. **Verify your Personal Access Token:**
   - Go to https://github.com/settings/tokens
   - Check that your token exists and hasn't expired
   - Ensure it has both `read:packages` and `write:packages` scopes
   - If needed, create a new token (classic) with these scopes

3. **Check your `~/.m2/settings.xml` format:**
   ```xml
   <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
             http://maven.apache.org/xsd/settings-1.0.0.xsd">
     <servers>
       <server>
         <id>github</id>
         <username>ACCURIDS</username>
         <password>YOUR_TOKEN_HERE</password>
       </server>
     </servers>
   </settings>
   ```
   - Make sure the `<id>` is exactly `github` (matches the repository ID in pom.xml)
   - The `<username>` should be your GitHub username (e.g., `ACCURIDS`)
   - The `<password>` should be your Personal Access Token (not your GitHub password)
   - No extra spaces or special characters

4. **Common issues:**
   - Using GitHub password instead of Personal Access Token
   - Token missing required scopes (`read:packages`, `write:packages`)
   - Username mismatch (must match your GitHub username)
   - XML formatting errors in settings.xml
   - Token expired or revoked

5. **Recreate settings.xml if needed:**
   ```bash
   # Backup existing file
   cp ~/.m2/settings.xml ~/.m2/settings.xml.backup
   
   # Create new one with correct format
   cat > ~/.m2/settings.xml <<EOF
   <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
             http://maven.apache.org/xsd/settings-1.0.0.xsd">
     <servers>
       <server>
         <id>github</id>
         <username>ACCURIDS</username>
         <password>YOUR_NEW_TOKEN_HERE</password>
       </server>
     </servers>
   </settings>
   EOF
   chmod 600 ~/.m2/settings.xml
   ```

### Error: 403 Forbidden
- Ensure the repository exists and you have write access
- Check that the package name matches the repository owner/name

### Error: Repository not found
- Verify the repository URL: `https://maven.pkg.github.com/ACCURIDS/sparql-generate`
- Make sure the repository is accessible
