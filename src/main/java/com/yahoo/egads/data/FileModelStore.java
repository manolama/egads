package com.yahoo.egads.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileModelStore implements ModelStore {
  private static final Logger LOG = LoggerFactory.getLogger(FileModelStore.class);
  
	String path;
	HashMap <String, Model> cache;
	
	public FileModelStore (String path) {
		File dir = new File (path);
		dir.mkdirs();
		this.path = path;
		cache = new HashMap<String, Model>();
    new File (path).mkdirs();
	}
	
	private String getFilename (String tag, Model.ModelType type) {
    String filename = tag.replaceAll("[^\\w_-]", "_");
    if (type == Model.ModelType.ANOMALY) {
      filename = "anomaly." + filename;
    } else if (type == Model.ModelType.FORECAST) {
      filename =  "forecast." + filename;
    }
    return filename;
	}

	@Override
	public void storeModel(String tag, Model m) {
		String filename = getFilename(tag, m.getModelType());
		String fqn = path + "/" + filename;
		try {
		  m.clearModified();
			ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream (fqn));
			o.writeObject(m);
			o.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Model retrieveModel(String tag, Model.ModelType type) {
	  String filename = getFilename(tag, type);
    if (cache.containsKey(filename)) {
      return cache.get(filename);
    }
		String fqn = path + "/" + filename;
		Model m = null;
		try {
			ObjectInputStream o = new ObjectInputStream(new FileInputStream(fqn));
			m =  (Model) o.readObject();
			o.close();
			cache.put(filename, m);
			return m;
		} catch (Exception e) {
			LOG.debug("Model not found: " + tag, e);
		}
		return null;
	}

	public void writeCachedModels() {
    for (String key : cache.keySet()) {
      Model model = cache.get(key);
      if (model.isModified()) {
//        The key always has the model type prepended - remove it before storing
        key = key.replaceFirst("[a-zA-Z]*\\.", "");
        storeModel(key, model);
      }
    }
  }
	
  public Collection<Model> getCachedModels() {
    return cache.values();
	}
  
}
