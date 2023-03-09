/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package util

import (
	"reflect"
	"runtime"
	"testing"
)

func Test_getProcessorInfos(t *testing.T) {
	type args struct {
		lsCPUStr string
	}
	tests := []struct {
		name    string
		args    args
		want    []ProcessorInfo
		wantErr bool
	}{
		{
			name:    "read empty lsCPUStr",
			args:    args{lsCPUStr: ""},
			want:    nil,
			wantErr: true,
		},
		{
			name: "do not panic for invalid lsCPUStr",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
0   0    0      -    -             yes
1   0    0      -    -             yes
`},
			want:    nil,
			wantErr: true,
		},
		{
			name: "do not panic for invalid cache info",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
0   0    0      0    -             yes
1   0    0      0    no cacheinfo  yes
`},
			want:    nil,
			wantErr: true,
		},
		{
			name: "do not panic for invalid cache info 1",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
0   0    0      0    3:3:3:-       yes
1   0    0      1    3:3:3:-       yes
`},
			want:    nil,
			wantErr: true,
		},
		{
			name: "read partial lsCPUStr",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
6   0    0      3    3:3:3:0       是
7   0    0      3    3:3:3:0       是
8   0    0      4    4:4:4:0       是
9   0    0      4    4:4:4:0       是
10  0    0      5    5:5:5:0       是
11  0    0      5    5:5:5:0       是
`,
			},
			want: []ProcessorInfo{
				{CPUID: 6, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "是"},
				{CPUID: 7, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "是"},
				{CPUID: 8, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "是"},
				{CPUID: 9, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "是"},
				{CPUID: 10, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "是"},
				{CPUID: 11, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "是"},
			},
			wantErr: false,
		},
		{
			name: "read lsCPUStr successfully 0",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
0   0    0      0    0:0:0:0       是
1   0    0      0    0:0:0:0       是
2   0    0      1    1:1:1:0       是
3   0    0      1    1:1:1:0       是
4   0    0      2    2:2:2:0       是
5   0    0      2    2:2:2:0       是
6   0    0      3    3:3:3:0       是
7   0    0      3    3:3:3:0       是
8   0    0      4    4:4:4:0       是
9   0    0      4    4:4:4:0       是
10  0    0      5    5:5:5:0       是
11  0    0      5    5:5:5:0       是
12  0    0      6    6:6:6:0       是
13  0    0      6    6:6:6:0       是
14  0    0      7    7:7:7:0       是
15  0    0      7    7:7:7:0       是
`,
			},
			want: []ProcessorInfo{
				{CPUID: 0, CoreID: 0, SocketID: 0, NodeID: 0, L1dl1il2: "0", L3: 0, Online: "是"},
				{CPUID: 1, CoreID: 0, SocketID: 0, NodeID: 0, L1dl1il2: "0", L3: 0, Online: "是"},
				{CPUID: 2, CoreID: 1, SocketID: 0, NodeID: 0, L1dl1il2: "1", L3: 0, Online: "是"},
				{CPUID: 3, CoreID: 1, SocketID: 0, NodeID: 0, L1dl1il2: "1", L3: 0, Online: "是"},
				{CPUID: 4, CoreID: 2, SocketID: 0, NodeID: 0, L1dl1il2: "2", L3: 0, Online: "是"},
				{CPUID: 5, CoreID: 2, SocketID: 0, NodeID: 0, L1dl1il2: "2", L3: 0, Online: "是"},
				{CPUID: 6, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "是"},
				{CPUID: 7, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "是"},
				{CPUID: 8, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "是"},
				{CPUID: 9, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "是"},
				{CPUID: 10, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "是"},
				{CPUID: 11, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "是"},
				{CPUID: 12, CoreID: 6, SocketID: 0, NodeID: 0, L1dl1il2: "6", L3: 0, Online: "是"},
				{CPUID: 13, CoreID: 6, SocketID: 0, NodeID: 0, L1dl1il2: "6", L3: 0, Online: "是"},
				{CPUID: 14, CoreID: 7, SocketID: 0, NodeID: 0, L1dl1il2: "7", L3: 0, Online: "是"},
				{CPUID: 15, CoreID: 7, SocketID: 0, NodeID: 0, L1dl1il2: "7", L3: 0, Online: "是"},
			},
			wantErr: false,
		},
		{
			name: "read lsCPUStr successfully 1",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE MAXMHZ    MINMHZ
0   0    0      0    0:0:0:0       yes    3000.0000 1200.0000
1   0    0      1    1:1:1:0       yes    3000.0000 1200.0000
2   0    0      2    2:2:2:0       yes    3000.0000 1200.0000
3   0    0      3    3:3:3:0       yes    3000.0000 1200.0000
4   0    0      4    4:4:4:0       yes    3000.0000 1200.0000
5   0    0      5    5:5:5:0       yes    3000.0000 1200.0000
6   0    0      6    6:6:6:0       yes    3000.0000 1200.0000
7   0    0      7    7:7:7:0       yes    3000.0000 1200.0000
8   0    0      8    8:8:8:0       yes    3000.0000 1200.0000
9   0    0      9    9:9:9:0       yes    3000.0000 1200.0000
10  0    0      10   10:10:10:0    yes    3000.0000 1200.0000
11  0    0      11   11:11:11:0    yes    3000.0000 1200.0000
12  0    0      12   12:12:12:0    yes    3000.0000 1200.0000
13  0    0      13   13:13:13:0    yes    3000.0000 1200.0000
14  0    0      14   14:14:14:0    yes    3000.0000 1200.0000
15  0    0      15   15:15:15:0    yes    3000.0000 1200.0000
16  0    1      16   16:16:16:1    yes    3000.0000 1200.0000
17  0    1      17   17:17:17:1    yes    3000.0000 1200.0000
18  0    1      18   18:18:18:1    yes    3000.0000 1200.0000
19  0    1      19   19:19:19:1    yes    3000.0000 1200.0000
20  0    1      20   20:20:20:1    yes    3000.0000 1200.0000
21  0    1      21   21:21:21:1    yes    3000.0000 1200.0000
22  0    1      22   22:22:22:1    yes    3000.0000 1200.0000
23  0    1      23   23:23:23:1    yes    3000.0000 1200.0000
24  0    1      24   24:24:24:1    yes    3000.0000 1200.0000
25  0    1      25   25:25:25:1    yes    3000.0000 1200.0000
26  0    1      26   26:26:26:1    yes    3000.0000 1200.0000
27  0    1      27   27:27:27:1    yes    3000.0000 1200.0000
28  0    1      28   28:28:28:1    yes    3000.0000 1200.0000
29  0    1      29   29:29:29:1    yes    3000.0000 1200.0000
30  0    1      30   30:30:30:1    yes    3000.0000 1200.0000
31  0    1      31   31:31:31:1    yes    3000.0000 1200.0000
32  0    0      0    0:0:0:0       yes    3000.0000 1200.0000
33  0    0      1    1:1:1:0       yes    3000.0000 1200.0000
34  0    0      2    2:2:2:0       yes    3000.0000 1200.0000
35  0    0      3    3:3:3:0       yes    3000.0000 1200.0000
36  0    0      4    4:4:4:0       yes    3000.0000 1200.0000
37  0    0      5    5:5:5:0       yes    3000.0000 1200.0000
38  0    0      6    6:6:6:0       yes    3000.0000 1200.0000
39  0    0      7    7:7:7:0       yes    3000.0000 1200.0000
40  0    0      8    8:8:8:0       yes    3000.0000 1200.0000
41  0    0      9    9:9:9:0       yes    3000.0000 1200.0000
42  0    0      10   10:10:10:0    yes    3000.0000 1200.0000
43  0    0      11   11:11:11:0    yes    3000.0000 1200.0000
44  0    0      12   12:12:12:0    yes    3000.0000 1200.0000
45  0    0      13   13:13:13:0    yes    3000.0000 1200.0000
46  0    0      14   14:14:14:0    yes    3000.0000 1200.0000
47  0    0      15   15:15:15:0    yes    3000.0000 1200.0000
48  0    1      16   16:16:16:1    yes    3000.0000 1200.0000
49  0    1      17   17:17:17:1    yes    3000.0000 1200.0000
50  0    1      18   18:18:18:1    yes    3000.0000 1200.0000
51  0    1      19   19:19:19:1    yes    3000.0000 1200.0000
52  0    1      20   20:20:20:1    yes    3000.0000 1200.0000
53  0    1      21   21:21:21:1    yes    3000.0000 1200.0000
54  0    1      22   22:22:22:1    yes    3000.0000 1200.0000
55  0    1      23   23:23:23:1    yes    3000.0000 1200.0000
56  0    1      24   24:24:24:1    yes    3000.0000 1200.0000
57  0    1      25   25:25:25:1    yes    3000.0000 1200.0000
58  0    1      26   26:26:26:1    yes    3000.0000 1200.0000
59  0    1      27   27:27:27:1    yes    3000.0000 1200.0000
60  0    1      28   28:28:28:1    yes    3000.0000 1200.0000
61  0    1      29   29:29:29:1    yes    3000.0000 1200.0000
62  0    1      30   30:30:30:1    yes    3000.0000 1200.0000
63  0    1      31   31:31:31:1    yes    3000.0000 1200.0000
`,
			},
			want: []ProcessorInfo{
				{CPUID: 0, CoreID: 0, SocketID: 0, NodeID: 0, L1dl1il2: "0", L3: 0, Online: "yes"},
				{CPUID: 32, CoreID: 0, SocketID: 0, NodeID: 0, L1dl1il2: "0", L3: 0, Online: "yes"},
				{CPUID: 1, CoreID: 1, SocketID: 0, NodeID: 0, L1dl1il2: "1", L3: 0, Online: "yes"},
				{CPUID: 33, CoreID: 1, SocketID: 0, NodeID: 0, L1dl1il2: "1", L3: 0, Online: "yes"},
				{CPUID: 2, CoreID: 2, SocketID: 0, NodeID: 0, L1dl1il2: "2", L3: 0, Online: "yes"},
				{CPUID: 34, CoreID: 2, SocketID: 0, NodeID: 0, L1dl1il2: "2", L3: 0, Online: "yes"},
				{CPUID: 3, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "yes"},
				{CPUID: 35, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "yes"},
				{CPUID: 4, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "yes"},
				{CPUID: 36, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "yes"},
				{CPUID: 5, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "yes"},
				{CPUID: 37, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "yes"},
				{CPUID: 6, CoreID: 6, SocketID: 0, NodeID: 0, L1dl1il2: "6", L3: 0, Online: "yes"},
				{CPUID: 38, CoreID: 6, SocketID: 0, NodeID: 0, L1dl1il2: "6", L3: 0, Online: "yes"},
				{CPUID: 7, CoreID: 7, SocketID: 0, NodeID: 0, L1dl1il2: "7", L3: 0, Online: "yes"},
				{CPUID: 39, CoreID: 7, SocketID: 0, NodeID: 0, L1dl1il2: "7", L3: 0, Online: "yes"},
				{CPUID: 8, CoreID: 8, SocketID: 0, NodeID: 0, L1dl1il2: "8", L3: 0, Online: "yes"},
				{CPUID: 40, CoreID: 8, SocketID: 0, NodeID: 0, L1dl1il2: "8", L3: 0, Online: "yes"},
				{CPUID: 9, CoreID: 9, SocketID: 0, NodeID: 0, L1dl1il2: "9", L3: 0, Online: "yes"},
				{CPUID: 41, CoreID: 9, SocketID: 0, NodeID: 0, L1dl1il2: "9", L3: 0, Online: "yes"},
				{CPUID: 10, CoreID: 10, SocketID: 0, NodeID: 0, L1dl1il2: "10", L3: 0, Online: "yes"},
				{CPUID: 42, CoreID: 10, SocketID: 0, NodeID: 0, L1dl1il2: "10", L3: 0, Online: "yes"},
				{CPUID: 11, CoreID: 11, SocketID: 0, NodeID: 0, L1dl1il2: "11", L3: 0, Online: "yes"},
				{CPUID: 43, CoreID: 11, SocketID: 0, NodeID: 0, L1dl1il2: "11", L3: 0, Online: "yes"},
				{CPUID: 12, CoreID: 12, SocketID: 0, NodeID: 0, L1dl1il2: "12", L3: 0, Online: "yes"},
				{CPUID: 44, CoreID: 12, SocketID: 0, NodeID: 0, L1dl1il2: "12", L3: 0, Online: "yes"},
				{CPUID: 13, CoreID: 13, SocketID: 0, NodeID: 0, L1dl1il2: "13", L3: 0, Online: "yes"},
				{CPUID: 45, CoreID: 13, SocketID: 0, NodeID: 0, L1dl1il2: "13", L3: 0, Online: "yes"},
				{CPUID: 14, CoreID: 14, SocketID: 0, NodeID: 0, L1dl1il2: "14", L3: 0, Online: "yes"},
				{CPUID: 46, CoreID: 14, SocketID: 0, NodeID: 0, L1dl1il2: "14", L3: 0, Online: "yes"},
				{CPUID: 15, CoreID: 15, SocketID: 0, NodeID: 0, L1dl1il2: "15", L3: 0, Online: "yes"},
				{CPUID: 47, CoreID: 15, SocketID: 0, NodeID: 0, L1dl1il2: "15", L3: 0, Online: "yes"},
				{CPUID: 16, CoreID: 16, SocketID: 1, NodeID: 0, L1dl1il2: "16", L3: 1, Online: "yes"},
				{CPUID: 48, CoreID: 16, SocketID: 1, NodeID: 0, L1dl1il2: "16", L3: 1, Online: "yes"},
				{CPUID: 17, CoreID: 17, SocketID: 1, NodeID: 0, L1dl1il2: "17", L3: 1, Online: "yes"},
				{CPUID: 49, CoreID: 17, SocketID: 1, NodeID: 0, L1dl1il2: "17", L3: 1, Online: "yes"},
				{CPUID: 18, CoreID: 18, SocketID: 1, NodeID: 0, L1dl1il2: "18", L3: 1, Online: "yes"},
				{CPUID: 50, CoreID: 18, SocketID: 1, NodeID: 0, L1dl1il2: "18", L3: 1, Online: "yes"},
				{CPUID: 19, CoreID: 19, SocketID: 1, NodeID: 0, L1dl1il2: "19", L3: 1, Online: "yes"},
				{CPUID: 51, CoreID: 19, SocketID: 1, NodeID: 0, L1dl1il2: "19", L3: 1, Online: "yes"},
				{CPUID: 20, CoreID: 20, SocketID: 1, NodeID: 0, L1dl1il2: "20", L3: 1, Online: "yes"},
				{CPUID: 52, CoreID: 20, SocketID: 1, NodeID: 0, L1dl1il2: "20", L3: 1, Online: "yes"},
				{CPUID: 21, CoreID: 21, SocketID: 1, NodeID: 0, L1dl1il2: "21", L3: 1, Online: "yes"},
				{CPUID: 53, CoreID: 21, SocketID: 1, NodeID: 0, L1dl1il2: "21", L3: 1, Online: "yes"},
				{CPUID: 22, CoreID: 22, SocketID: 1, NodeID: 0, L1dl1il2: "22", L3: 1, Online: "yes"},
				{CPUID: 54, CoreID: 22, SocketID: 1, NodeID: 0, L1dl1il2: "22", L3: 1, Online: "yes"},
				{CPUID: 23, CoreID: 23, SocketID: 1, NodeID: 0, L1dl1il2: "23", L3: 1, Online: "yes"},
				{CPUID: 55, CoreID: 23, SocketID: 1, NodeID: 0, L1dl1il2: "23", L3: 1, Online: "yes"},
				{CPUID: 24, CoreID: 24, SocketID: 1, NodeID: 0, L1dl1il2: "24", L3: 1, Online: "yes"},
				{CPUID: 56, CoreID: 24, SocketID: 1, NodeID: 0, L1dl1il2: "24", L3: 1, Online: "yes"},
				{CPUID: 25, CoreID: 25, SocketID: 1, NodeID: 0, L1dl1il2: "25", L3: 1, Online: "yes"},
				{CPUID: 57, CoreID: 25, SocketID: 1, NodeID: 0, L1dl1il2: "25", L3: 1, Online: "yes"},
				{CPUID: 26, CoreID: 26, SocketID: 1, NodeID: 0, L1dl1il2: "26", L3: 1, Online: "yes"},
				{CPUID: 58, CoreID: 26, SocketID: 1, NodeID: 0, L1dl1il2: "26", L3: 1, Online: "yes"},
				{CPUID: 27, CoreID: 27, SocketID: 1, NodeID: 0, L1dl1il2: "27", L3: 1, Online: "yes"},
				{CPUID: 59, CoreID: 27, SocketID: 1, NodeID: 0, L1dl1il2: "27", L3: 1, Online: "yes"},
				{CPUID: 28, CoreID: 28, SocketID: 1, NodeID: 0, L1dl1il2: "28", L3: 1, Online: "yes"},
				{CPUID: 60, CoreID: 28, SocketID: 1, NodeID: 0, L1dl1il2: "28", L3: 1, Online: "yes"},
				{CPUID: 29, CoreID: 29, SocketID: 1, NodeID: 0, L1dl1il2: "29", L3: 1, Online: "yes"},
				{CPUID: 61, CoreID: 29, SocketID: 1, NodeID: 0, L1dl1il2: "29", L3: 1, Online: "yes"},
				{CPUID: 30, CoreID: 30, SocketID: 1, NodeID: 0, L1dl1il2: "30", L3: 1, Online: "yes"},
				{CPUID: 62, CoreID: 30, SocketID: 1, NodeID: 0, L1dl1il2: "30", L3: 1, Online: "yes"},
				{CPUID: 31, CoreID: 31, SocketID: 1, NodeID: 0, L1dl1il2: "31", L3: 1, Online: "yes"},
				{CPUID: 63, CoreID: 31, SocketID: 1, NodeID: 0, L1dl1il2: "31", L3: 1, Online: "yes"},
			},
			wantErr: false,
		},
		{
			name: "read lsCPUStr successfully 2",
			args: args{
				lsCPUStr: `
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
0   0    0      0    0:0:0:0       yes
1   0    0      1    1:1:1:0       yes
2   0    0      2    2:2:2:0       yes
3   0    0      3    3:3:3:0       yes
4   0    0      4    4:4:4:0       yes
5   0    0      5    5:5:5:0       yes
6   0    0      6    6:6:6:0       yes
7   0    0      7    7:7:7:0       yes
8   1    0      8    8:8:8:1       yes
9   1    0      9    9:9:9:1       yes
10  1    0      10   10:10:10:1    yes
11  1    0      11   11:11:11:1    yes
12  1    0      12   12:12:12:1    yes
13  1    0      13   13:13:13:1    yes
14  1    0      14   14:14:14:1    yes
15  1    0      15   15:15:15:1    yes
16  2    1      16   16:16:16:2    yes
17  2    1      17   17:17:17:2    yes
18  2    1      18   18:18:18:2    yes
19  2    1      19   19:19:19:2    yes
20  2    1      20   20:20:20:2    yes
21  2    1      21   21:21:21:2    yes
22  2    1      22   22:22:22:2    yes
23  2    1      23   23:23:23:2    yes
24  3    1      24   24:24:24:3    yes
25  3    1      25   25:25:25:3    yes
26  3    1      26   26:26:26:3    yes
27  3    1      27   27:27:27:3    yes
28  3    1      28   28:28:28:3    yes
29  3    1      29   29:29:29:3    yes
30  3    1      30   30:30:30:3    yes
31  3    1      31   31:31:31:3    yes
`,
			},
			want: []ProcessorInfo{
				{CPUID: 0, CoreID: 0, SocketID: 0, NodeID: 0, L1dl1il2: "0", L3: 0, Online: "yes"},
				{CPUID: 1, CoreID: 1, SocketID: 0, NodeID: 0, L1dl1il2: "1", L3: 0, Online: "yes"},
				{CPUID: 2, CoreID: 2, SocketID: 0, NodeID: 0, L1dl1il2: "2", L3: 0, Online: "yes"},
				{CPUID: 3, CoreID: 3, SocketID: 0, NodeID: 0, L1dl1il2: "3", L3: 0, Online: "yes"},
				{CPUID: 4, CoreID: 4, SocketID: 0, NodeID: 0, L1dl1il2: "4", L3: 0, Online: "yes"},
				{CPUID: 5, CoreID: 5, SocketID: 0, NodeID: 0, L1dl1il2: "5", L3: 0, Online: "yes"},
				{CPUID: 6, CoreID: 6, SocketID: 0, NodeID: 0, L1dl1il2: "6", L3: 0, Online: "yes"},
				{CPUID: 7, CoreID: 7, SocketID: 0, NodeID: 0, L1dl1il2: "7", L3: 0, Online: "yes"},
				{CPUID: 8, CoreID: 8, SocketID: 0, NodeID: 1, L1dl1il2: "8", L3: 1, Online: "yes"},
				{CPUID: 9, CoreID: 9, SocketID: 0, NodeID: 1, L1dl1il2: "9", L3: 1, Online: "yes"},
				{CPUID: 10, CoreID: 10, SocketID: 0, NodeID: 1, L1dl1il2: "10", L3: 1, Online: "yes"},
				{CPUID: 11, CoreID: 11, SocketID: 0, NodeID: 1, L1dl1il2: "11", L3: 1, Online: "yes"},
				{CPUID: 12, CoreID: 12, SocketID: 0, NodeID: 1, L1dl1il2: "12", L3: 1, Online: "yes"},
				{CPUID: 13, CoreID: 13, SocketID: 0, NodeID: 1, L1dl1il2: "13", L3: 1, Online: "yes"},
				{CPUID: 14, CoreID: 14, SocketID: 0, NodeID: 1, L1dl1il2: "14", L3: 1, Online: "yes"},
				{CPUID: 15, CoreID: 15, SocketID: 0, NodeID: 1, L1dl1il2: "15", L3: 1, Online: "yes"},
				{CPUID: 16, CoreID: 16, SocketID: 1, NodeID: 2, L1dl1il2: "16", L3: 2, Online: "yes"},
				{CPUID: 17, CoreID: 17, SocketID: 1, NodeID: 2, L1dl1il2: "17", L3: 2, Online: "yes"},
				{CPUID: 18, CoreID: 18, SocketID: 1, NodeID: 2, L1dl1il2: "18", L3: 2, Online: "yes"},
				{CPUID: 19, CoreID: 19, SocketID: 1, NodeID: 2, L1dl1il2: "19", L3: 2, Online: "yes"},
				{CPUID: 20, CoreID: 20, SocketID: 1, NodeID: 2, L1dl1il2: "20", L3: 2, Online: "yes"},
				{CPUID: 21, CoreID: 21, SocketID: 1, NodeID: 2, L1dl1il2: "21", L3: 2, Online: "yes"},
				{CPUID: 22, CoreID: 22, SocketID: 1, NodeID: 2, L1dl1il2: "22", L3: 2, Online: "yes"},
				{CPUID: 23, CoreID: 23, SocketID: 1, NodeID: 2, L1dl1il2: "23", L3: 2, Online: "yes"},
				{CPUID: 24, CoreID: 24, SocketID: 1, NodeID: 3, L1dl1il2: "24", L3: 3, Online: "yes"},
				{CPUID: 25, CoreID: 25, SocketID: 1, NodeID: 3, L1dl1il2: "25", L3: 3, Online: "yes"},
				{CPUID: 26, CoreID: 26, SocketID: 1, NodeID: 3, L1dl1il2: "26", L3: 3, Online: "yes"},
				{CPUID: 27, CoreID: 27, SocketID: 1, NodeID: 3, L1dl1il2: "27", L3: 3, Online: "yes"},
				{CPUID: 28, CoreID: 28, SocketID: 1, NodeID: 3, L1dl1il2: "28", L3: 3, Online: "yes"},
				{CPUID: 29, CoreID: 29, SocketID: 1, NodeID: 3, L1dl1il2: "29", L3: 3, Online: "yes"},
				{CPUID: 30, CoreID: 30, SocketID: 1, NodeID: 3, L1dl1il2: "30", L3: 3, Online: "yes"},
				{CPUID: 31, CoreID: 31, SocketID: 1, NodeID: 3, L1dl1il2: "31", L3: 3, Online: "yes"},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getProcessorInfos(tt.args.lsCPUStr)
			if (err != nil) != tt.wantErr {
				t.Errorf("getProcessorInfos wantErr %v but got err %s", tt.wantErr, err)
			}
			if !reflect.DeepEqual(tt.want, got) {
				t.Errorf("getProcessorInfos want %v but got %v", tt.want, got)
			}
		})
	}
}

func Test_calculateCPUTotalInfo(t *testing.T) {
	type args struct {
		processorInfos []ProcessorInfo
	}
	tests := []struct {
		name string
		args args
		want *CPUTotalInfo
	}{
		{
			name: "parse processorInfos",
			args: args{processorInfos: []ProcessorInfo{
				{CPUID: 6, CoreID: 3, SocketID: 0, NodeID: 0},
				{CPUID: 7, CoreID: 3, SocketID: 0, NodeID: 0},
				{CPUID: 8, CoreID: 4, SocketID: 0, NodeID: 0},
				{CPUID: 9, CoreID: 4, SocketID: 0, NodeID: 0},
				{CPUID: 10, CoreID: 5, SocketID: 0, NodeID: 0},
				{CPUID: 11, CoreID: 5, SocketID: 0, NodeID: 0},
			}},
			want: &CPUTotalInfo{
				NumberCPUs:    6,
				NumberCores:   3,
				NumberSockets: 1,
				NumberNodes:   1,
				NumberL3s:     1,
			},
		},
		{
			name: "parse processorInfos 1",
			args: args{processorInfos: []ProcessorInfo{
				{CPUID: 0, CoreID: 0, SocketID: 0, NodeID: 0},
				{CPUID: 1, CoreID: 1, SocketID: 0, NodeID: 0},
				{CPUID: 2, CoreID: 2, SocketID: 1, NodeID: 0},
				{CPUID: 3, CoreID: 3, SocketID: 1, NodeID: 0},
				{CPUID: 4, CoreID: 4, SocketID: 2, NodeID: 1},
				{CPUID: 5, CoreID: 5, SocketID: 2, NodeID: 1},
				{CPUID: 6, CoreID: 6, SocketID: 3, NodeID: 1},
				{CPUID: 7, CoreID: 7, SocketID: 3, NodeID: 1},
			}},
			want: &CPUTotalInfo{
				NumberCPUs:    8,
				NumberCores:   8,
				NumberSockets: 4,
				NumberNodes:   2,
				NumberL3s:     1,
			},
		},
		{
			name: "parse processorInfos 2",
			args: args{processorInfos: []ProcessorInfo{
				{CPUID: 0, CoreID: 0, SocketID: 0, NodeID: 0, L3: 0},
				{CPUID: 1, CoreID: 1, SocketID: 0, NodeID: 0, L3: 0},
				{CPUID: 2, CoreID: 2, SocketID: 1, NodeID: 0, L3: 0},
				{CPUID: 3, CoreID: 3, SocketID: 1, NodeID: 0, L3: 0},
				{CPUID: 4, CoreID: 4, SocketID: 2, NodeID: 1, L3: 1},
				{CPUID: 5, CoreID: 5, SocketID: 2, NodeID: 1, L3: 1},
				{CPUID: 6, CoreID: 6, SocketID: 3, NodeID: 1, L3: 1},
				{CPUID: 7, CoreID: 7, SocketID: 3, NodeID: 1, L3: 1},
			}},
			want: &CPUTotalInfo{
				NumberCPUs:    8,
				NumberCores:   8,
				NumberSockets: 4,
				NumberNodes:   2,
				NumberL3s:     2,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := calculateCPUTotalInfo(tt.args.processorInfos)
			if !reflect.DeepEqual(tt.want, got) {
				t.Errorf("calculateCPUTotalInfo want %v but got %v", tt.want, got)
			}
		})
	}
}

func Test_GetLocalCPUInfo(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Log("Ignore non-Linux environment")
		return
	}
	localCPUInfo, err := GetLocalCPUInfo()
	if err != nil {
		t.Error("failed to get local CPU info: ", err)
	}
	t.Log("get local CPU info ", localCPUInfo)
}
