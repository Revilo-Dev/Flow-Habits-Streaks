const path = require("path");
const fs = require("fs");
const migrationsDir = path.resolve(__dirname, "../../../../uhabits-core/assets/main/migrations");
const testAssetsDir = path.resolve(__dirname, "../../../../uhabits-core/assets/test");
const fontsDir = path.resolve(__dirname, "../../../../uhabits-core/assets/main/fonts");

function saveFileMiddleware(req, res, next) {
    if (req.url !== "/save-file" || req.method !== "POST") return next();
    const filePath = req.headers["x-file-path"];
    if (!filePath) {
        res.writeHead(400);
        res.end("Missing X-File-Path header");
        return;
    }
    const chunks = [];
    req.on("data", chunk => chunks.push(chunk));
    req.on("end", () => {
        const dir = path.dirname(filePath);
        fs.mkdirSync(dir, { recursive: true });
        fs.writeFileSync(filePath, Buffer.concat(chunks));
        res.writeHead(200);
        res.end("OK");
    });
}

config.set({
    beforeMiddleware: ["saveFile"],
    plugins: (config.plugins || []).concat([
        { "middleware:saveFile": ["value", saveFileMiddleware] }
    ]),
    browserNoActivityTimeout: 120000,
    browserDisconnectTimeout: 30000,
    browserDisconnectTolerance: 3,
    client: {
        mocha: {
            timeout: 30000
        }
    },
    proxies: {
        '/migrations/': '/absolute' + migrationsDir + '/',
        '/test-assets/': '/absolute' + testAssetsDir + '/',
        '/fonts/': '/absolute' + fontsDir + '/'
    }
});

config.files.push(
    { pattern: migrationsDir + '/*.sql', included: false, served: true, watched: false },
    { pattern: testAssetsDir + '/**/*', included: false, served: true, watched: false },
    { pattern: fontsDir + '/*.ttf', included: false, served: true, watched: false }
);

// Use sql-asm.js (pure JS, no WASM binary loading) to avoid karma-webpack
// issue #498 where WASM assets in webpack's temp output dir are not served.
config.webpack.resolve = config.webpack.resolve || {};
config.webpack.resolve.fallback = Object.assign(
    config.webpack.resolve.fallback || {},
    { fs: false, path: false, crypto: false }
);
config.webpack.resolve.alias = Object.assign(
    config.webpack.resolve.alias || {},
    { "sql.js": path.resolve(__dirname, "../../node_modules/sql.js/dist/sql-asm.js") }
);
