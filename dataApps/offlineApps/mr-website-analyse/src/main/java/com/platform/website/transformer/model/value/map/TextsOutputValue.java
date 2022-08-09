package com.platform.website.transformer.model.value.map;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class TextsOutputValue extends BaseStatsValueWritable {
  private KpiType kpiType;
  private String uuid;
  private String sid;

  public TextsOutputValue() {
    super();
  }

  public TextsOutputValue(String uuid, String sid) {
    super();
    this.uuid = uuid;
    this.sid = sid;
  }


  @Override
  public KpiType getKpi() {
    return this.kpiType;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    this.internalWrite(dataOutput, this.uuid);
    this.internalWrite(dataOutput, this.sid);

  }

  private void internalWrite(DataOutput out, String value) throws IOException {
    if (StringUtils.isEmpty(value)) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeUTF(value);
    }
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.uuid = this.internalRead(dataInput);
    this.sid  = this.internalRead(dataInput);
  }

  private String internalRead(DataInput input) throws IOException {
    return input.readBoolean() ? input.readUTF() : null;
  }

}
