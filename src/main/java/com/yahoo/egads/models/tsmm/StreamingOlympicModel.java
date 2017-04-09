/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// Olympic scoring model considers the average of the last k weeks
// (dropping the b highest and lowest values) as the current prediction.

package com.yahoo.egads.models.tsmm;

import com.yahoo.egads.data.*;
import com.yahoo.egads.data.TimeSeries.Entry;

import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;

import com.yahoo.egads.utilities.FileUtils;

public class StreamingOlympicModel extends TimeSeriesStreamingModel {
  private static final Logger LOG = LoggerFactory.getLogger(StreamingOlympicModel.class);
  
    // methods ////////////////////////////////////////////////

  private static final long serialVersionUID = 1L;

  private HashMap<Long, Double> model;
  protected int period;
  protected double smoothingFactor;
    
    public StreamingOlympicModel() {
        super();
        smoothingFactor = 0.4;
        period = 86400 * 7;
        model = new HashMap<Long, Double>();
    }
    public StreamingOlympicModel(double smoothingFactor, int period) {
        super();
        this.smoothingFactor = smoothingFactor;
        this.period = period;
        this.model = new HashMap<Long, Double>();
    }

    public void reset() {
        model = new HashMap<Long, Double>();
    }
    
    private long timeToModelTime (long time) {
      if (period == 86400 * 7) {
        return weeklyOffset(time);
      }
      if (period == 86400) {
        return dailyOffset(time);
      }
      return time % period;
    }
    
    public void update (TimeSeries.Entry entry) {
      long modelTime = timeToModelTime(entry.time);
      if (model.containsKey(modelTime)) {
        model.put(modelTime, model.get(modelTime) * (1 - smoothingFactor) + entry.value * smoothingFactor);
      } else {
        model.put(modelTime,  (double)entry.value);
      }
      modified = true;
    }
    
    public double predict (TimeSeries.Entry entry) {
      long modelTime = timeToModelTime(entry.time);
      double prediction;
      if (model.containsKey(modelTime)) {
        prediction = model.get(modelTime);
      } else {
        prediction = entry.value;
      }
      double error = entry.value - prediction;
    sumErr += error;
        sumAbsErr += Math.abs(error);
        sumAbsPercentErr += 100 * Math.abs(error / entry.value);
        sumErrSquared += error * error;
        processedPoints++;
        return prediction;
    }
    
    private void runSeries (TimeSeries.DataSequence data) {
      clearErrorStats();
      for (TimeSeries.Entry entry : data) {
        predict(entry);
        update(entry);
      }
    }
    
    public void train(TimeSeries.DataSequence data) {
      StreamingOlympicModel winner = null;
      double sf = 0.0;
      for (sf = 0.0; sf <= 1; sf += 0.1) {
        StreamingOlympicModel m = new StreamingOlympicModel(sf, this.period);
        m.runSeries(data);
          LOG.debug ("Testing Smoothing Factor " + String.format("%.2f", m.smoothingFactor) + " -> "+ m.errorSummaryString());
        if (betterThan(m, winner)) {
          winner = m;
        }
      }
      double min = winner.smoothingFactor - 0.09;
      if (min < 0) min = 0;
      double max = winner.smoothingFactor + 0.09;
      if (max >= 1) max = .99;
      for (sf = min; sf <= max; sf += 0.01) {
        StreamingOlympicModel m = new StreamingOlympicModel(sf, this.period);
        m.runSeries(data);
          LOG.debug ("Testing Smoothing Factor " + String.format("%.2f", m.smoothingFactor) + " -> "+ m.errorSummaryString());
        if (betterThan(m, winner)) {
          winner = m;
        }
      }
      this.smoothingFactor = winner.smoothingFactor;
      reset();
      runSeries(data);
      LOG.debug ("Winner: Smoothing Factor = " + String.format("%.2f", this.smoothingFactor));
    }

    public double getSmoothingFactor() {
    return smoothingFactor;
  }

  public void setSmoothingFactor(double smoothingFactor) {
    this.smoothingFactor = smoothingFactor;
  }

  public void update(TimeSeries.DataSequence data) {

    }

    public String getModelName() {
        return "OlympicModel";
    }

    public void predict(TimeSeries.DataSequence sequence) throws Exception {
      return;
    }
}