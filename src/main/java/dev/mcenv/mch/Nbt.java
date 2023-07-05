package dev.mcenv.mch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

sealed interface Nbt {
  byte id();

  static Nbt load(final DataInput in, final byte id) throws IOException {
    switch (id) {
      case 1 -> {
        return new Byte(in.readByte());
      }
      case 2 -> {
        return new Short(in.readShort());
      }
      case 3 -> {
        return new Int(in.readInt());
      }
      case 4 -> {
        return new Long(in.readLong());
      }
      case 5 -> {
        return new Float(in.readFloat());
      }
      case 6 -> {
        return new Double(in.readDouble());
      }
      case 7 -> {
        final var size = in.readInt();
        final var values = new byte[size];
        in.readFully(values);
        return new ByteArray(values);
      }
      case 8 -> {
        return new String(in.readUTF());
      }
      case 9 -> {
        final var type = in.readByte();
        final var size = in.readInt();
        final var elements = new ArrayList<Nbt>(size);
        for (var i = 0; i < size; ++i) {
          elements.add(load(in, type));
        }
        return new List(elements);
      }
      case 10 -> {
        final var elements = new HashMap<java.lang.String, Nbt>();
        byte type;
        while ((type = in.readByte()) != 0) {
          final var name = in.readUTF();
          elements.put(name, load(in, type));
        }
        return new Compound(elements);
      }
      case 11 -> {
        final var size = in.readInt();
        final var values = new int[size];
        for (var i = 0; i < size; ++i) {
          values[i] = in.readInt();
        }
        return new IntArray(values);
      }
      case 12 -> {
        final var size = in.readInt();
        final var values = new long[size];
        for (var i = 0; i < size; ++i) {
          values[i] = in.readLong();
        }
        return new LongArray(values);
      }
      default -> throw new IllegalStateException("Unexpected id: " + id);
    }
  }

  static void write(final Nbt nbt, final Path path) throws IOException {
    try (final var out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(path))))) {
      out.writeByte(nbt.id());
      out.writeUTF("");
      nbt.store(out);
    }
  }

  static Compound read(final Path path) throws IOException {
    try (final var in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path))))) {
      final var type = in.readByte();
      in.readUTF();
      return (Compound) load(in, type);
    }
  }

  void store(final DataOutput out) throws IOException;

  record Byte(
    byte value
  ) implements Nbt {
    @Override
    public byte id() {
      return 1;
    }

    @Override
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
      out.writeUTF(value);
    }
  }

  record List(
    java.util.List<? extends Nbt> elements
  ) implements Nbt {
    @Override
    public byte id() {
      return 9;
    }

    @Override
    public void store(final DataOutput out) throws IOException {
      out.writeByte(elements.get(0).id());
      out.writeInt(elements.size());
      for (final Nbt nbt : elements) {
        nbt.store(out);
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
    public void store(final DataOutput out) throws IOException {
      for (final var entry : elements.entrySet()) {
        out.writeByte(entry.getValue().id());
        out.writeUTF(entry.getKey());
        entry.getValue().store(out);
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
    public void store(final DataOutput out) throws IOException {
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
    public void store(final DataOutput out) throws IOException {
      out.writeInt(values.length);
      for (final var value : values) {
        out.writeLong(value);
      }
    }
  }
}
