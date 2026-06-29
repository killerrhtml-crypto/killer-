import express from 'express';
import path from 'path';
import fs from 'fs';
import cors from 'cors';

let db: any = null;
let firebaseInitialized = false;

async function tryInitFirebase() {
  try {
    const configPath = path.join(process.cwd(), 'firebase-applet-config.json');
    if (!fs.existsSync(configPath)) {
      console.warn('Advertencia: firebase-applet-config.json no encontrado.');
      return;
    }

    const firebaseApp = await import('firebase/app');
    const firebaseFirestore = await import('firebase/firestore');

    if (firebaseFirestore && typeof firebaseFirestore.setLogLevel === 'function') {
      firebaseFirestore.setLogLevel('silent');
    }

    const firebaseConfig = JSON.parse(fs.readFileSync(configPath, 'utf-8'));
    const appFirebase = firebaseApp.initializeApp(firebaseConfig);
    db = firebaseFirestore.getFirestore(appFirebase, firebaseConfig.firestoreDatabaseId || '(default)');
    firebaseInitialized = true;
    console.log('Firebase Firestore conectado exitosamente al ID de proyecto:', firebaseConfig.projectId);
  } catch (error) {
    console.warn('Inicialización de Firebase omitida o fallida (modo fallback a memoria):', (error && (error as Error).message) || error);
    db = null;
  }
}

const initialUsers = [
  { deviceId: 'DEV-101', username: 'Carlos Chofer (ATM 1)', status: 'active', plan_days: 30, expiry_date: Date.now() + 15 * 24 * 3600 * 1000 },
  { deviceId: 'DEV-102', username: 'Miguel ATM', status: 'active', plan_days: 15, expiry_date: Date.now() + 5 * 24 * 3600 * 1000 },
  { deviceId: 'DEV-103', username: 'Juan Perez', status: 'blocked', plan_days: 15, expiry_date: Date.now() - 2 * 24 * 3600 * 1000 },
  { deviceId: 'DEV-999', username: 'Dispositivo Prueba Sigilo', status: 'active', plan_days: 30, expiry_date: Date.now() + 28 * 24 * 3600 * 1000 },
];

let memoryUsers: any[] = [...initialUsers];

async function getUsersFromFirestore() {
  if (!db) {
    return memoryUsers;
  }
  try {
    const { collection, getDocs, doc, setDoc } = await import('firebase/firestore');
    const usersCol = collection(db, 'users');
    const snapshot = await getDocs(usersCol);
    if (snapshot.empty) {
      for (const u of initialUsers) {
        await setDoc(doc(db, 'users', u.deviceId), u);
      }
      memoryUsers = [...initialUsers];
      return memoryUsers;
    }
    const list: any[] = [];
    snapshot.forEach(d => list.push(d.data()));
    memoryUsers = list;
    return list;
  } catch (err) {
    console.warn('Error leyendo Firestore, usando memoria:', err);
    return memoryUsers;
  }
}

async function startServer() {
  await tryInitFirebase();

  const app = express();
  const PORT = 3000;

  app.use(cors());
  app.use(express.json());

  app.post('/api/check-access', async (req, res) => {
    const { device_id, api_key } = req.body;

    if (device_id === 'nosotrosrd' && api_key === 'Diosamor') {
      res.json({
        status: 'GRANTED',
        isAdmin: true,
        token: 'admin_master_token_999',
        expiryTime: Date.now() + 24 * 3600 * 1000,
        config: { minPrice: 50, maxDistance: 15, blockedZones: [] }
      });
      return;
    }

    if (device_id && device_id.startsWith('DEV-') && api_key === 'SECRET_123') {
      try {
        const users = await getUsersFromFirestore();
        const user = users.find((u: any) => u.deviceId === device_id);
        if (user && user.status === 'active' && user.expiry_date > Date.now()) {
          res.json({
            status: 'GRANTED',
            isAdmin: false,
            token: 'auth_token_' + device_id,
            expiryTime: user.expiry_date,
            config: { minPrice: 50, maxDistance: 15, blockedZones: ['Zona Norte', 'Centro Histórico'] }
          });
          return;
        }
      } catch (err) {
        console.warn('Error check-access:', err);
      }
    }

    res.json({ status: 'DENIED' });
  });

  app.get('/api/admin/users', async (req, res) => {
    try {
      const users = await getUsersFromFirestore();
      res.json(users);
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/admin/users/update', async (req, res) => {
    const { deviceId, daysToAdd, status } = req.body;
    try {
      await getUsersFromFirestore();
      const userIndex = memoryUsers.findIndex(u => u.deviceId === deviceId);
      if (userIndex === -1) {
        res.status(404).json({ success: false, error: 'Usuario no encontrado' });
        return;
      }
      const data = memoryUsers[userIndex];
      if (typeof daysToAdd === 'number' && daysToAdd > 0) {
        const baseTime = Math.max(Date.now(), data.expiry_date || 0);
        data.expiry_date = baseTime + daysToAdd * 24 * 3600 * 1000;
        data.plan_days = daysToAdd;
      }
      if (status) {
        data.status = status;
      }
      if (db) {
        try {
          const { doc, updateDoc } = await import('firebase/firestore');
          const userRef = doc(db, 'users', deviceId);
          await updateDoc(userRef, { expiry_date: data.expiry_date, plan_days: data.plan_days, status: data.status });
        } catch (e) {
          console.warn('Error updating Firestore user (continuando con memoria):', e);
        }
      }
      res.json({ success: true, user: data });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  });

  app.post('/api/admin/users/create', async (req, res) => {
    const { deviceId, username, plan_days } = req.body;
    const days = plan_days || 15;
    const newId = deviceId || `DEV-${Math.floor(Math.random() * 900) + 100}`;
    await getUsersFromFirestore();
    if (memoryUsers.some(u => u.deviceId === newId)) {
      res.status(400).json({ success: false, error: 'El ID de dispositivo ya existe' });
      return;
    }
    const newUser = {
      deviceId: newId,
      username: username || 'Nuevo Chofer',
      status: 'active' as const,
      plan_days: days,
      expiry_date: Date.now() + days * 24 * 3600 * 1000
    };
    memoryUsers.push(newUser);
    if (db) {
      try {
        const { doc, setDoc } = await import('firebase/firestore');
        await setDoc(doc(db, 'users', newId), newUser);
      } catch (e) {
        console.warn('Error creando usuario en Firestore (continuando con memoria):', e);
      }
    }
    res.json({ success: true, user: newUser });
  });

  app.get('/api/poll-rides', (req, res) => {
    const zones = ['Zona Norte', 'Centro Histórico', 'Zona Sur', 'Distrito Financiero', 'Aeropuerto'];
    const randomZone = zones[Math.floor(Math.random() * zones.length)];
    const randomPrice = Math.floor(Math.random() * 1000) + 150;
    const randomDistance = Math.floor(Math.random() * 30) + 1;

    res.json({
      price: randomPrice,
      distance: randomDistance,
      zone: randomZone,
      destination: 'Aeropuerto'
    });
  });

  app.get('/api/history', (req, res) => {
    const history = [
      { id: 1, date: '2026-06-25T08:30:00Z', price: 450.00, distance: 8, zone: 'Centro Histórico', destination: 'Aeropuerto' },
      { id: 2, date: '2026-06-24T14:15:00Z', price: 220.00, distance: 3, zone: 'Zona Sur', destination: 'Centro Histórico' },
      { id: 3, date: '2026-06-23T09:00:00Z', price: 850.00, distance: 18, zone: 'Aeropuerto', destination: 'Distrito Financiero' },
    ];
    res.json(history);
  });

  if (process.env.NODE_ENV !== 'production') {
    try {
      const viteModuleName = 'vite' as const;
      const viteModule: any = await import(viteModuleName);
      const createViteServer = viteModule.createServer;
      const vite = await createViteServer({
        server: { middlewareMode: true },
        appType: 'spa',
      });
      app.use((vite as any).middlewares);
      console.log('Vite dev middleware cargado dinámicamente.');
    } catch (err) {
      console.warn('Vite no inicializado en compilación de producción.', err);
    }
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
