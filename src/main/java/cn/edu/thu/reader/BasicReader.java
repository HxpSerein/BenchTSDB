package cn.edu.thu.reader;

import cn.edu.thu.common.Config;
import cn.edu.thu.common.Record;
import cn.edu.thu.common.Schema;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicReader implements Iterator<List<Record>> {

  public static final String DEVICE_PREFIX = "root.group_0.d_";

  private static Logger logger = LoggerFactory.getLogger(BasicReader.class);
  protected Config config;
  protected List<String> files;
  protected BufferedReader reader;
  protected List<String> cachedLines;
  protected int currentFileIndex = 0;

  protected String currentFile;
  protected String currentDeviceId;

  public BasicReader(Config config) {
    this.config = config;
  }

  public BasicReader(Config config, List<String> files) {
    this.config = config;
    this.files = files;
    cachedLines = new ArrayList<>(config.BATCH_SIZE);
    try {
      reader = new BufferedReader(new FileReader(files.get(currentFileIndex)));
      currentFile = files.get(currentFileIndex);
      logger.info("start to read {}-th file {}", currentFileIndex, currentFile);
      onFileOpened();
    } catch (Exception e) {
      logger.error("meet exception when init file: {}", currentFile);
      e.printStackTrace();
    }
  }

  public boolean hasNext() {

    if (!cachedLines.isEmpty()) {
      return true;
    }

    try {
      String line;
      while (true) {

        if(reader == null) {
          return false;
        }

        line = reader.readLine();

        // current file end
        if(line == null) {

          // current file has been resolved, read next file
          if(cachedLines.isEmpty()) {
            if (currentFileIndex < files.size() - 1) {
              currentFile = files.get(++currentFileIndex);
              logger.info("start to read {}-th file {}", currentFileIndex, currentFile);
              reader.close();
              reader = new BufferedReader(new FileReader(currentFile));
              onFileOpened();
              continue;
            } else {
              // no more file to read
              reader.close();
              reader = null;
              break;
            }
          } else {
            // resolve current file
            return true;
          }
        } else if (line.isEmpty()){
          continue;
        }

        // read a line, cache it
        cachedLines.add(line);
        if (cachedLines.size() >= config.BATCH_SIZE) {
          break;
        }
      }
    } catch (Exception e) {
      logger.error("read file {} failed", currentFile, e);
      return false;
    }

    return !cachedLines.isEmpty();
  }


  /**
   * convert the cachedLines to Record list
   */
  protected abstract List<Record> convertCachedLinesToRecords();

  public List<Record> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    // shuffle the list with 10% probability
    if(randomChance()){
      shuffleList();
    }

    List<Record> records = convertCachedLinesToRecords();
//    cachedLines.clear();

    return records;
  }

  /**
   * return true with 10% probability
   */
  public boolean randomChance() {
    Random random = new Random();
    return random.nextDouble() < 0.1;
  }

  /**
   * shuffle the cachedLines
   */
  public void shuffleList() {
    Collections.shuffle(cachedLines);
  }

  /**
   * initialize when start reading a file
   * maybe skip the first lines
   * maybe init the tagValue(deviceId) from file name
   */
  public abstract void onFileOpened() throws Exception;

  public abstract Schema getCurrentSchema();
}
