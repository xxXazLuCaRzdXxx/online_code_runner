const Docker = require('dockerode');
const fs = require('fs').promises;
const path = require('path');
const os = require('os');

const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const crypto = require('crypto');

const app = express();
const port = 5000;

app.use(cors());
app.use(bodyParser.json());

const docker = new Docker();
const containerPool = {};

const SUPPORTED_LANGUAGES = {
  python: { image: 'python:3.9', cmd: ['python3'], ext: 'py' },
  javascript: { image: 'node:14', cmd: ['node'], ext: 'js' },
  ruby: { image: 'ruby:2.7', cmd: ['ruby'], ext: 'rb' },
};

async function initializeContainerPool() {
  for (const [lang, config] of Object.entries(SUPPORTED_LANGUAGES)) {
    containerPool[lang] = await docker.createContainer({
      Image: config.image,
      Cmd: ['/bin/sh'],
      Tty: true,
      OpenStdin: true,
      StdinOnce: false,
      HostConfig: {
        AutoRemove: true,
        Memory: 128 * 1024 * 1024, // 128 MB limit
        CpuPeriod: 100000,
        CpuQuota: 50000, // Limit to 50% of CPU
        Binds: [`${os.tmpdir()}:/tmp`],  // Mount the host's temp directory to the container's /tmp
      },
    });
    await containerPool[lang].start();
  }
}

initializeContainerPool().catch(console.error);

app.post('/run', async (req, res) => {
  const { code, language, input } = req.body;

  if (!SUPPORTED_LANGUAGES[language]) {
    return res.status(400).json({ error: 'Unsupported language' });
  }

  const { cmd, ext } = SUPPORTED_LANGUAGES[language];
  const container = containerPool[language];

  if (!container) {
    return res.status(503).json({ error: 'Container not available' });
  }

  // os.tmpdir() for cross-platform file writing
  const uniqueId = crypto.randomBytes(16).toString('hex');

  const tempDir = os.tmpdir();
  const scriptFileName = `${uniqueId}.${ext}`; 
  const scriptFilePath = path.join(tempDir, scriptFileName);
  const inputFileName = `${uniqueId}.txt`
  const inputFilePath = path.join(tempDir, `${uniqueId}.txt`);


  try {
    await fs.writeFile(scriptFilePath, code);
    await fs.writeFile(inputFilePath, input);

    const execConfig = {
      Cmd: [
        'sh',
        '-c',
        `${cmd[0]} /tmp/${scriptFileName} < /tmp/${inputFileName}`
      ],
      AttachStdout: true,
      AttachStderr: true,
    };

    const start = process.hrtime();

    const exec = await container.exec(execConfig);
    const stream = await exec.start();

    let output = '';
    stream.on('data', (chunk) => {
        output += chunk.toString().replace(/[^\x20-\x7E\n]/g, '');
    });

    await new Promise((resolve) => stream.on('end', resolve));

    const [seconds, nanoseconds] = process.hrtime(start);
    const executionTime = seconds * 1000 + nanoseconds / 1e6; // Convert to milliseconds

    await fs.unlink(scriptFilePath);
    await fs.unlink(inputFilePath);

    res.json({ output, executionTime });
  } catch (error) {
    console.error('Error running code:', error);
    res.status(500).json({ error: 'An error occurred while running the code' });
  }
});

app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});
