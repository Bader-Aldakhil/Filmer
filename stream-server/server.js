const express = require('express');
const cors    = require('cors');
const WebTorrent = require('webtorrent');

const app    = express();
// High-performance P2P configuration
const client = new WebTorrent({
  tracker: true,
  dht: {
    bootstrap: [
      'router.bittorrent.com:6881',
      'dht.transmissionbt.com:6881',
      'router.utorrent.com:6881'
    ]
  },
  lsd: true,
  utp: true
});

app.use(cors());

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.get('/stream', (req, res) => {
  const magnet = req.query.magnet;
  if (!magnet) return res.status(400).json({ error: 'magnet param required' });

  req.socket.setTimeout(3600000); // 1 hour timeout for movie streaming

  let torrent = client.get(magnet);
  
  if (!torrent) {
    console.log('Adding torrent:', magnet.slice(0, 60) + '...');
    torrent = client.add(magnet, { 
      path: process.env.TEMP || 'C:\\temp\\filmer-torrents',
      announce: [
        'udp://tracker.opentrackr.org:1337/announce',
        'udp://open.stealth.si:80/announce',
        'udp://tracker.torrent.eu.org:451/announce',
        'udp://tracker.moeking.me:6969/announce',
        'udp://exodus.desync.com:6969/announce',
        'http://tracker.openbittorrent.com:80/announce',
        'wss://tracker.btorrent.xyz',
        'wss://tracker.openwebtorrent.com'
      ]
    });
  }

  const onReady = () => {
    console.log('Metadata ready for:', torrent.name);
    pipeFile(torrent, req, res);
  };

  if (torrent.ready) {
    onReady();
  } else {
    torrent.once('ready', onReady);
    
    // 5 minute timeout for discovery (BitTorrent can be slow initially)
    const timeout = setTimeout(() => {
      if (!res.headersSent && !torrent.ready) {
        console.error('Metadata timeout for:', torrent.infoHash);
        torrent.removeListener('ready', onReady);
        res.status(503).json({ error: 'Peer discovery taking too long. Please wait or try again.' });
      }
    }, 300000);
    
    torrent.once('ready', () => clearTimeout(timeout));
  }
});

function pipeFile(torrent, req, res) {
  const videoExts = ['.mp4', '.mkv', '.avi', '.mov', '.webm'];
  let file = torrent.files
    .filter(f => videoExts.some(ext => f.name.toLowerCase().endsWith(ext)))
    .reduce((a, b) => (a && a.length > b.length ? a : b), null);

  if (!file) {
    file = torrent.files.reduce((a, b) => (a.length > b.length ? a : b), torrent.files[0]);
  }

  if (!file) return res.status(503).json({ error: 'No video file found' });

  const fileSize = file.length;
  const range    = req.headers.range;

  if (range) {
    const parts = range.replace(/bytes=/, "").split("-");
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
    const chunksize = (end - start) + 1;
    
    res.writeHead(206, {
      'Content-Range': `bytes ${start}-${end}/${fileSize}`,
      'Accept-Ranges': 'bytes',
      'Content-Length': chunksize,
      'Content-Type': 'video/mp4',
      'Access-Control-Allow-Origin': '*'
    });
    file.createReadStream({ start, end }).pipe(res);
  } else {
    res.writeHead(200, {
      'Content-Length': fileSize,
      'Content-Type': 'video/mp4',
      'Access-Control-Allow-Origin': '*'
    });
    file.createReadStream().pipe(res);
  }
}

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => console.log(`Professional P2P Stream Server running on port ${PORT}`));
