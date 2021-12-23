# TUID (Time-based Unique Identifier)
UUID（Universally Unique Identifier） 是国际标准化组织（ISO）提出的一个概念，能在所有空间和时间上被视为唯一的标识，一般来说，可以保证这个值是真正唯一的，即任何地方产生的任意一个UUID都不会有相同的值。 UUID作为通用唯一识别码，在计算机系统中，确实承担了非常重要的功能：确保ID的唯一性。

按照ISO组织的定义，UUID原本是一个128比特的数值，但由于long整数是64位，以及128比特的数值的可阅读性差，因而UUID在计算机的计算组件中以及存储组件中，大都是以UUID转化成的字符串而存在、传递、保存，标准的UUID字符串格式为：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (8-4-4-4-12，其中每个 x 是 0-9 或 a-f 范围内的一个十六进制的数字)，即32位的16进制字符串。

UUID虽然使用广泛，但一个长度为36的字符串只承担了确保唯一性的功能，实在有些“浪费”。一个UUID是否能在保留其唯一性之外，进一步提供一些其他的特性：比如时间戳？并能基于时间戳进行排序？而且看起来还像一个UUID？TUID，基于时间序列的唯一识别码就是一个实现这个功能的、相比UUID更轻量级的解决方案。

# TUID的结构
TUID只能是以字符串而最终存在，其形式也是xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx（8-4-4-4-12），其中每个x不再是一个十六进制的数字，而是 0-9 或 A-Z 或 a-z 范围内的一个62进制的数字。相比起16进制，62进制是更加充分利用了ANSI编码的基本字符，自然，一个32位的62进制字符串可以比32位的16进制的字符串，包含更多信息。

一个UUID实际上可以表示为两个64位的long整数，一个64位的long整数是可以由11位的62进制的数字所完全代表（实际上，62的11次方 约为 2的64次方的 2.6倍，可以化简为 31的11次方 和 2的53次方之比），那么一个UUID所包含的两个long整数，只需22位的62进制就可以代表。

TUID的另外10位62进制数又会进一步分拆成两部分：7位所代表的时间戳（距离1970年1月1日的毫秒数）和3位所代表的顺序码。经过简单计算，7位62进制数 / 一年的总毫秒数 ～= 101年，也就是数100年之内，这个时间戳是不会重复。3位顺序码，是在22位的62进制所代表的UUID相同，而且7位时间戳毫秒数也相同情况下的，基于零的顺序码。

总体来说，TUID的32位是如下分配：
<li>第一位到第7位是时间戳，</li>
<li>第9位到第19位，和第22位到第32位，分别代表UUID的两个long整数，</li>
<li>第8位、第20位、第21位组成3位顺序码</li>

# TUID的唯一性：
<li>对于不同机器、不同进程、不同线程：各自TUID所自动生成的UUID种子是不相同的，因而各自TUID必然是不同的；</li>
<li>对于同一机器、同一进程、同一线程之中，多次生成的TUID，首先如果时间戳不一样，自然各自TUID也是不同；如果两次请求TUID均是相同的时间戳，那就依赖于3位顺序码进行区分；如果3位顺序码超过3位62进制的最大值，那就重新生成一个新的UUID作为种子。</li>

# TUID的基于时间排序：
<li>由于TUID前七位代表时间戳，而这个前七位的字符串ANSI排序完全与时间戳的毫秒数排序相同，因而TUID的字符串排序基本上也就是等价于时间戳排序</li>

# TUID的不可预测性：
<li>由于TUID所包含的UUID本身是不可预测，因而TUID也有某种不可预测性；</li>
<li>当一个TUID的UUID种子使用超过一定时间之后（10分钟到100分钟之间的毫秒数，基于随机数而生成），会自动生成一个新的UUID种子。</li>

# TUID的高性能：
<li>基于ThreadLocal，确保每个线程均有其独有的UUID种子。而当UUID种子不发生变化时，每次生成TUID，实际上只需重新计算出时间戳和顺序码，其计算量是很轻量级。</li>
<li>事实上，针对TUID的生成和UUID的生成，分别进行3千万次压测测试，单次生成平均所耗的时间分别为200纳秒和520纳秒，也就是TUID生成时间约为UUID生成时间的40%。</li>

# 已知问题及应对之道
显然，TUID是大小写字母敏感的。但有些持久层（比如MySQL）缺省情况下对于值也是不区分大小写，那么持久化Span的时候，就有小概率遇上主键冲突的问题：Duplicate entry 'xxx' for key primary。

相应的解决方案有二：去掉唯一性的约束；或者创建表的时候声明相应字段是大小写敏感或binary类型。

比如，对于下面的两个表test1和test2,
<p>
  CREATE TABLE test1 (<br>
    trace_id        varchar(40) not null,<br>
    span_id         varchar(40) not null,<br>
    PRIMARY KEY(trace_id, span_id)<br>
  ) default charset=utf8;
  
  CREATE TABLE test2 (<br>
    trace_id        varchar(40) <b>binary</b> not null,<br>
    span_id         varchar(40) not null,<br>
    PRIMARY KEY(trace_id, span_id)<br>
  ) default charset=utf8;<br>
</p>
那么当对上述两表均插入如下两行数据的时候，<br>
insert into test (trace_id, span_id) values ('AAA', '1');<br>
insert into test (trace_id, span_id) values ('aaa', '1');<br>
第一行数据均能成功插入，但第二行数据就只能test2才能成功插入。
