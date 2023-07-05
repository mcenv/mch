package dev.mcenv.mch;

import java.io.DataOutput;
import java.io.IOException;

sealed interface Nbt {
  byte id();

  void write(final DataOutput out) throws IOException;

  record Byte(
    byte value
  ) implements Nbt {
    @Override
    public byte id() {
      return 1;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeByte(value);
    }
  }

  record Short(
    short value
  ) implements Nbt {
    @Override
    public byte id() {
      return 2;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeShort(value);
    }
  }

  record Int(
    int value
  ) implements Nbt {
    @Override
    public byte id() {
      return 3;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeInt(value);
    }
  }

  record Long(
    long value
  ) implements Nbt {
    @Override
    public byte id() {
      return 4;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeLong(value);
    }
  }

  record Float(
    float value
  ) implements Nbt {
    @Override
    public byte id() {
      return 5;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeFloat(value);
    }
  }

  record Double(
    double value
  ) implements Nbt {
    @Override
    public byte id() {
      return 6;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeDouble(value);
    }
  }

  record ByteArray(
    byte[] values
  ) implements Nbt {
    @Override
    public byte id() {
      return 7;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeInt(values.length);
      out.write(values);
    }
  }

  record String(
    java.lang.String value
  ) implements Nbt {
    @Override
    public byte id() {
      return 8;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeUTF(value);
    }
  }

  record List(
    java.util.List<Nbt> elements
  ) implements Nbt {
    @Override
    public byte id() {
      return 9;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeByte(elements.get(0).id());
      out.writeInt(elements.size());
      for (final Nbt nbt : elements) {
        nbt.write(out);
      }
    }
  }

  record Compound(
    java.util.Map<java.lang.String, Nbt> elements
  ) implements Nbt {
    @Override
    public byte id() {
      return 10;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      for (final var entry : elements.entrySet()) {
        out.writeByte(entry.getValue().id());
        out.writeUTF(entry.getKey());
        entry.getValue().write(out);
      }
      out.writeByte(0);
    }
  }

  record IntArray(
    int[] values
  ) implements Nbt {
    @Override
    public byte id() {
      return 11;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeInt(values.length);
      for (final var value : values) {
        out.writeInt(value);
      }
    }
  }

  record LongArray(
    long[] values
  ) implements Nbt {
    @Override
    public byte id() {
      return 12;
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeInt(values.length);
      for (final var value : values) {
        out.writeLong(value);
      }
    }
  }
}
