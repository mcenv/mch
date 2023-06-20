package dev.mcenv.mch;

import java.io.Serializable;

sealed interface Message extends Serializable permits Message.RunResult, Message.MaxCommandChainLengthExceeded {
  record RunResult(
    double[] scores
  ) implements Message {
  }

  record MaxCommandChainLengthExceeded() implements Message {
  }
}
