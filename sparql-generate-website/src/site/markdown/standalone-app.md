# SPARQL-Generate Standalone Application

A standalone version of the [SPARQL-Generate playground](playground.html) can be run as a [electron.js](https://electronjs.org/) application.

## Run the packaged distribution

This standalone application uses the SPARQL-Generate executable JAR, which requires Java JRE 1.8 or above. 

1. Download the version that corresponds to the platform you are using, 
1. Unzip the archive,
1. Run the executable,

[![Download for Windows](windows.png) Download for Windows (tested, 98 Mo)](https://ci.mines-stetienne.fr/SPARQL-Generate-playground-win32-ia32.zip), md5 `d0ed504d984eacd1b0b43f6f994e9014`, sha1 `c20592ecfc2d05f107ac91657479d227c255a559`

[![Download for Linux](linux.png) Download for Linux (tested, 105 Mo)](https://ci.mines-stetienne.fr/SPARQL-Generate-playground-linux-x64.zip), md5 `5fc8778032c08b1b942ae308a9c8f362`, sha1 `2421b7731c7f19ae348046ba21e9368a56346c40`

[![Download for Mac-darwin-or-mas](mac.png) Download for Mac-darwin (not tested, 314 Mo)](https://ci.mines-stetienne.fr/SPARQL-Generate-playground-darwin-x64.zip), md5 `b5d1db330c9fd2658f948e52926d5a3d`, sha1 `31e33843e81a27525a0fae565d8dd311205f5667`

[![Download for Mac-darwin-or-mas](mac.png) Download for Mac-mas (not tested, 253 Mo)](https://ci.mines-stetienne.fr/SPARQL-Generate-playground-mas-x64.zip), md5 `d66e90af953b40f7184dcdddb658162b`, sha1 `ba445eabf2af44b0260013d3f82f8857a4afe57a`

## Run from the sources

The SPARQL-Generate standalone application sources are available on GitHub: https://github.com/sparql-generate/sparql-generate-app

To get and run the SPARQL-Generate standalone application you'll need [Git](https://git-scm.com) and [Node.js](https://nodejs.org/en/download/) (which comes with [npm](http://npmjs.com)) installed on your computer.

We tested with nodejs 8.11.2 and npm 5.6.0, and cannot assure the app will run with another version of nodejs. 

From your command line:

```bash
git clone https://github.com/sparql-generate/sparql-generate-app
cd sparql-generate-app

# Install dependencies
npm install

# Run the app
npm start
```

Note: If you're using Linux Bash for Windows, [see this guide](https://www.howtogeek.com/261575/how-to-run-graphical-linux-desktop-applications-from-windows-10s-bash-shell/) or use `node` from the command prompt.