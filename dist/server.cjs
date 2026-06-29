var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));

// server.ts
var import_express = __toESM(require("express"));
var import_path = __toESM(require("path"));
var import_fs = __toESM(require("fs"));
var import_cors = __toESM(require("cors"));
var db = null;
var firebaseInitialized = false;
async function tryInitFirebase() {
  try {
    const configPath = import_path.default.join(process.cwd(), "firebase-applet-config.json");
    if (!import_fs.default.existsSync(configPath)) {
      console.warn("Advertencia: firebase-applet-config.json no encontrado.");
      return;
    }
    const firebaseApp = await import("firebase/app");
    const firebaseFirestore = await import("firebase/firestore");
    if (firebaseFirestore && typeof firebaseFirestore.setLogLevel === "function") {
      firebaseFirestore.setLogLevel("silent");
    }
    const firebaseConfig = JSON.parse(import_fs.default.readFileSync(configPath, "utf-8"));
    const appFirebase = firebaseApp.initializeApp(firebaseConfig);
    db = firebaseFirestore.getFirestore(appFirebase, firebaseConfig.firestoreDatabaseId || "(default)");
    firebaseInitialized = true;
    console.log("Firebase Firestore conectado exitosamente al ID de proyecto:", firebaseConfig.projectId);
  } catch (error) {
    console.warn("Inicializaci\xF3n de Firebase omitida o fallida (modo fallback a memoria):", error && error.message || error);
    db = null;
  }
}
var initialUsers = [
  { deviceId: "DEV-101", username: "Carlos Chofer (ATM 1)", status: "active", plan_days: 30, expiry_date: Date.now() + 15 * 24 * 3600 * 1e3 },
  { deviceId: "DEV-102", username: "Miguel ATM", status: "active", plan_days: 15, expiry_date: Date.now() + 5 * 24 * 3600 * 1e3 },
  { deviceId: "DEV-103", username: "Juan Perez", status: "blocked", plan_days: 15, expiry_date: Date.now() - 2 * 24 * 3600 * 1e3 },
  { deviceId: "DEV-999", username: "Dispositivo Prueba Sigilo", status: "active", plan_days: 30, expiry_date: Date.now() + 28 * 24 * 3600 * 1e3 }
];
var memoryUsers = [...initialUsers];
async function getUsersFromFirestore() {
  if (!db) {
    return memoryUsers;
  }
  try {
    const { collection, getDocs, doc, setDoc } = await import("firebase/firestore");
    const usersCol = collection(db, "users");
    const snapshot = await getDocs(usersCol);
    if (snapshot.empty) {
      for (const u of initialUsers) {
        await setDoc(doc(db, "users", u.deviceId), u);
      }
      memoryUsers = [...initialUsers];
      return memoryUsers;
    }
    const list = [];
    snapshot.forEach((d) => list.push(d.data()));
    memoryUsers = list;
    return list;
  } catch (err) {
    console.warn("Error leyendo Firestore, usando memoria:", err);
    return memoryUsers;
  }
}
async function startServer() {
  await tryInitFirebase();
  const app = (0, import_express.default)();
  const PORT = 3e3;
  app.use((0, import_cors.default)());
  app.use(import_express.default.json());
  app.post("/api/check-access", async (req, res) => {
    const { device_id, api_key } = req.body;
    if (device_id === "nosotrosrd" && api_key === "Diosamor") {
      res.json({
        status: "GRANTED",
        isAdmin: true,
        token: "admin_master_token_999",
        expiryTime: Date.now() + 24 * 3600 * 1e3,
        config: { minPrice: 50, maxDistance: 15, blockedZones: [] }
      });
      return;
    }
    if (device_id && device_id.startsWith("DEV-") && api_key === "SECRET_123") {
      try {
        const users = await getUsersFromFirestore();
        const user = users.find((u) => u.deviceId === device_id);
        if (user && user.status === "active" && user.expiry_date > Date.now()) {
          res.json({
            status: "GRANTED",
            isAdmin: false,
            token: "auth_token_" + device_id,
            expiryTime: user.expiry_date,
            config: { minPrice: 50, maxDistance: 15, blockedZones: ["Zona Norte", "Centro Hist\xF3rico"] }
          });
          return;
        }
      } catch (err) {
        console.warn("Error check-access:", err);
      }
    }
    res.json({ status: "DENIED" });
  });
  app.get("/api/admin/users", async (req, res) => {
    try {
      const users = await getUsersFromFirestore();
      res.json(users);
    } catch (err) {
      res.status(500).json({ error: err.message });
    }
  });
  app.post("/api/admin/users/update", async (req, res) => {
    const { deviceId, daysToAdd, status } = req.body;
    try {
      await getUsersFromFirestore();
      const userIndex = memoryUsers.findIndex((u) => u.deviceId === deviceId);
      if (userIndex === -1) {
        res.status(404).json({ success: false, error: "Usuario no encontrado" });
        return;
      }
      const data = memoryUsers[userIndex];
      if (typeof daysToAdd === "number" && daysToAdd > 0) {
        const baseTime = Math.max(Date.now(), data.expiry_date || 0);
        data.expiry_date = baseTime + daysToAdd * 24 * 3600 * 1e3;
        data.plan_days = daysToAdd;
      }
      if (status) {
        data.status = status;
      }
      if (db) {
        try {
          const { doc, updateDoc } = await import("firebase/firestore");
          const userRef = doc(db, "users", deviceId);
          await updateDoc(userRef, { expiry_date: data.expiry_date, plan_days: data.plan_days, status: data.status });
        } catch (e) {
          console.warn("Error updating Firestore user (continuando con memoria):", e);
        }
      }
      res.json({ success: true, user: data });
    } catch (e) {
      res.status(500).json({ success: false, error: e.message });
    }
  });
  app.post("/api/admin/users/create", async (req, res) => {
    const { deviceId, username, plan_days } = req.body;
    const days = plan_days || 15;
    const newId = deviceId || `DEV-${Math.floor(Math.random() * 900) + 100}`;
    await getUsersFromFirestore();
    if (memoryUsers.some((u) => u.deviceId === newId)) {
      res.status(400).json({ success: false, error: "El ID de dispositivo ya existe" });
      return;
    }
    const newUser = {
      deviceId: newId,
      username: username || "Nuevo Chofer",
      status: "active",
      plan_days: days,
      expiry_date: Date.now() + days * 24 * 3600 * 1e3
    };
    memoryUsers.push(newUser);
    if (db) {
      try {
        const { doc, setDoc } = await import("firebase/firestore");
        await setDoc(doc(db, "users", newId), newUser);
      } catch (e) {
        console.warn("Error creando usuario en Firestore (continuando con memoria):", e);
      }
    }
    res.json({ success: true, user: newUser });
  });
  app.get("/api/poll-rides", (req, res) => {
    const zones = ["Zona Norte", "Centro Hist\xF3rico", "Zona Sur", "Distrito Financiero", "Aeropuerto"];
    const randomZone = zones[Math.floor(Math.random() * zones.length)];
    const randomPrice = Math.floor(Math.random() * 1e3) + 150;
    const randomDistance = Math.floor(Math.random() * 30) + 1;
    res.json({
      price: randomPrice,
      distance: randomDistance,
      zone: randomZone,
      destination: "Aeropuerto"
    });
  });
  app.get("/api/history", (req, res) => {
    const history = [
      { id: 1, date: "2026-06-25T08:30:00Z", price: 450, distance: 8, zone: "Centro Hist\xF3rico", destination: "Aeropuerto" },
      { id: 2, date: "2026-06-24T14:15:00Z", price: 220, distance: 3, zone: "Zona Sur", destination: "Centro Hist\xF3rico" },
      { id: 3, date: "2026-06-23T09:00:00Z", price: 850, distance: 18, zone: "Aeropuerto", destination: "Distrito Financiero" }
    ];
    res.json(history);
  });
  if (process.env.NODE_ENV !== "production") {
    try {
      const viteModuleName = "vite";
      const viteModule = await import(viteModuleName);
      const createViteServer = viteModule.createServer;
      const vite = await createViteServer({
        server: { middlewareMode: true },
        appType: "spa"
      });
      app.use(vite.middlewares);
      console.log("Vite dev middleware cargado din\xE1micamente.");
    } catch (err) {
      console.warn("Vite no inicializado en compilaci\xF3n de producci\xF3n.", err);
    }
  } else {
    const distPath = import_path.default.join(process.cwd(), "dist");
    app.use(import_express.default.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(import_path.default.join(distPath, "index.html"));
    });
  }
  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}
startServer();
//# sourceMappingURL=server.cjs.map
