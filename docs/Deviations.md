## Count-based deviations

Card counting affects the basic strategy. The following deviations are implemented
in `blackjack-gui` (I will slowly add more). Use "Coach mode" with "Include deviations" to verify your play!

| Your Hand | Dealer's Upcard | Basic strategy        | Deviation | Index |
| :-------- | :-------------- | :-------------------- | :-------- | :---- |
| 2-10      | A               | Don't take insurance  | Take      | +3    |
| A         | A               | Don't take even money | Take      | +3    |
| 16        | 10              | Hit                   | Stand     | 0+    |
| 12        | 2               | Hit                   | Stand     | +3    |
| 12        | 3               | Hit                   | Stand     | +2    |
| 12        | 4               | Stand                 | Hit       | 0-    |
| A,4       | 4               | Double                | Hit       | 0-    |

Where 0- means any negative running count, 0+ means any positive running count, and +X means true count of X or greater.