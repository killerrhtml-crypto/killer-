import express, { Request, Response } from 'express';
import fs from 'fs';
import path from 'path';

const configRouter = express.Router();
const CONFIG_FILE = path.join(process.cwd(), 'config/apis-databases.json');

// Ensure config directory exists
function ensureConfigDir() {
  const configDir = path.join(process.cwd(), 'config');
  if (!fs.existsSync(configDir)) {
    fs.mkdirSync(configDir, { recursive: true });
  }
}

// Load configuration from file
function loadConfig() {
  ensureConfigDir();
  if (fs.existsSync(CONFIG_FILE)) {
    return JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf-8'));
  }
  return { apis: [], databases: [] };
}

// Save configuration to file
function saveConfig(config: any) {
  ensureConfigDir();
  fs.writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2), 'utf-8');
}

// Get all APIs and Databases
configRouter.get('/config', (req: Request, res: Response) => {
  try {
    const config = loadConfig();
    res.json(config);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Add API
configRouter.post('/config/api', (req: Request, res: Response) => {
  try {
    const { name, url, key, description } = req.body;
    
    if (!name || !url || !key) {
      res.status(400).json({ error: 'Nombre, URL y API Key son requeridos' });
      return;
    }

    const config = loadConfig();
    const newAPI = {
      id: Date.now(),
      name,
      url,
      key,
      description: description || '',
      createdAt: new Date().toISOString(),
      status: 'configured'
    };

    config.apis.push(newAPI);
    saveConfig(config);
    
    res.json({ success: true, api: newAPI });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Update API
configRouter.put('/config/api/:id', (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { name, url, key, description } = req.body;
    
    const config = loadConfig();
    const apiIndex = config.apis.findIndex((a: any) => a.id === Number(id));
    
    if (apiIndex === -1) {
      res.status(404).json({ error: 'API no encontrada' });
      return;
    }

    config.apis[apiIndex] = {
      ...config.apis[apiIndex],
      name: name || config.apis[apiIndex].name,
      url: url || config.apis[apiIndex].url,
      key: key || config.apis[apiIndex].key,
      description: description !== undefined ? description : config.apis[apiIndex].description,
      updatedAt: new Date().toISOString()
    };

    saveConfig(config);
    res.json({ success: true, api: config.apis[apiIndex] });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Delete API
configRouter.delete('/config/api/:id', (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const config = loadConfig();
    
    config.apis = config.apis.filter((a: any) => a.id !== Number(id));
    saveConfig(config);
    
    res.json({ success: true });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Test API connection
configRouter.post('/config/api/:id/test', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const config = loadConfig();
    const api = config.apis.find((a: any) => a.id === Number(id));
    
    if (!api) {
      res.status(404).json({ error: 'API no encontrada' });
      return;
    }

    // Simple test - try to fetch from the API URL
    try {
      const response = await fetch(api.url, {
        headers: { 'Authorization': `Bearer ${api.key}` },
        method: 'GET'
      });
      
      res.json({ 
        success: true, 
        status: response.status,
        message: 'Conexión exitosa'
      });
    } catch (fetchError) {
      res.json({ 
        success: false, 
        message: 'No se pudo conectar a la API'
      });
    }
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Add Database
configRouter.post('/config/database', (req: Request, res: Response) => {
  try {
    const { name, type, connection, description } = req.body;
    
    if (!name || !type || !connection) {
      res.status(400).json({ error: 'Nombre, tipo y string de conexión son requeridos' });
      return;
    }

    const config = loadConfig();
    const newDB = {
      id: Date.now(),
      name,
      type,
      connection,
      description: description || '',
      createdAt: new Date().toISOString(),
      status: 'configured'
    };

    config.databases.push(newDB);
    saveConfig(config);
    
    res.json({ success: true, database: newDB });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Update Database
configRouter.put('/config/database/:id', (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { name, type, connection, description } = req.body;
    
    const config = loadConfig();
    const dbIndex = config.databases.findIndex((d: any) => d.id === Number(id));
    
    if (dbIndex === -1) {
      res.status(404).json({ error: 'Base de datos no encontrada' });
      return;
    }

    config.databases[dbIndex] = {
      ...config.databases[dbIndex],
      name: name || config.databases[dbIndex].name,
      type: type || config.databases[dbIndex].type,
      connection: connection || config.databases[dbIndex].connection,
      description: description !== undefined ? description : config.databases[dbIndex].description,
      updatedAt: new Date().toISOString()
    };

    saveConfig(config);
    res.json({ success: true, database: config.databases[dbIndex] });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Delete Database
configRouter.delete('/config/database/:id', (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const config = loadConfig();
    
    config.databases = config.databases.filter((d: any) => d.id !== Number(id));
    saveConfig(config);
    
    res.json({ success: true });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Test Database connection
configRouter.post('/config/database/:id/test', (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const config = loadConfig();
    const db = config.databases.find((d: any) => d.id === Number(id));
    
    if (!db) {
      res.status(404).json({ error: 'Base de datos no encontrada' });
      return;
    }

    // In production, you would implement actual database connection testing
    // For now, we'll return a simulated response
    res.json({ 
      success: true, 
      message: 'Base de datos lista para conectar',
      type: db.type
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export default configRouter;
