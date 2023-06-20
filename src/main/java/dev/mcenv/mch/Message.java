package dev.mcenv.mch;

import java.io.Serializable;

sealed interface Message extends Serializable permits Message.RunResult {
  record RunResult(
    double[] scores
  ) implements Message {
  }
}
