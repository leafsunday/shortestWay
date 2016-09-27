计算全路网最短路径
=
根据基础数据设计算法，计算全路网全部站点之间的最短路径；<br/>

题目说明：
* 最短路径：两点间距离最短的路径。
* 在高速路网中，任意两点间的路径走法有很多，但最短路径的里程是最短的；
* 站点：路网中现有的所有站点（474个）；全部最短路径条数（理论上）：474*473 = 224202条（实际上要比这个少一些，有些站点之间由于方向性的问题是不通的）；
* 联通性 & 里程：列出了路网中任意两两相邻的站点，及站点之间的里程；
* 回避路径：高速路网有些道路由于立交桥或者互通的设计，某种特定的路径是不能走的，如果探索最短路径的时候，包含上述路径，则此条路径无效（要换其他走法）；
* 验证示例：大家可以用这几条路径进行算法测试，如果都能计算出来，则可以进行全路网路径测试；