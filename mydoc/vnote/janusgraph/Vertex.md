# IDManager
## 分析概念
```
relationCountBound = partitionBits==0?Long.MAX_VALUE:(1L << (TOTAL_BITS - partitionBits));//relationCount 边界
        assert VertexIDType.NormalVertex.offset()>0;
        vertexCountBound = (1L << (TOTAL_BITS - partitionBits - USERVERTEX_PADDING_BITWIDTH));// vertexCount 边界， 可以理解为总的vertex的最大量


        partitionOffset = Long.SIZE - partitionBits;
```

JanusGraphElement id bit

```
 /* ########################################################
                   User Relations and Vertices
       ########################################################  */

     /*		--- JanusGraphElement id bit format ---
      *  [ 0 | count | partition | ID padding (if any) ]
     */

 /**
     *bit mask- Description (+ indicates defined type, * indicates proper & defined type)
     *
     *      0 - + User created Vertex
     *    000 -     * Normal vertices
     *    010 -     * Partitioned vertices
     *    100 -     * Unmodifiable (e.g. TTL'ed) vertices
     *    110 -     + Reserved for additional vertex type
     *      1 - + Invisible
     *     11 -     * Invisible (user created/triggered) Vertex [for later]
     *     01 -     + Schema related vertices
     *    101 -         + Schema Type vertices
     *   0101 -             + Relation Type vertices
     *  00101 -                 + Property Key
     * 000101 -                     * User Property Key
     * 100101 -                     * System Property Key
     *  10101 -                 + Edge Label
     * 010101 -                     * User Edge Label
     * 110101 -                     * System Edge Label
     *   1101 -             Other Type vertices
     *  01101 -                 * Vertex Label
     *    001 -         Non-Type vertices
     *   1001 -             * Generic Schema Vertex
     *   0001 -             Reserved for future
     *
     *
     */
```
offset() 偏移量代表固定的那个值占几位， 比如上面的Normal vertices 它的固定值就是000（3位）, 则offset（）就是3， suffix（）是0, 000b
```
public final long addPadding(long count) {
            assert offset()>0;
            Preconditions.checkArgument(count>0 && count<(1L <<(TOTAL_BITS-offset())),"Count out of range for type [%s]: %s",this,count);
            return (count << offset()) | suffix();
        }
```
关于ID padding的值可以看到它是 (count << offset() ) | suffix(), 相当于往左平移offset()位， 因为最后的那几个offset()位必须为默认值。正如Partitontioned vertices最后三位是010一样。

```
public final boolean is(long id) {
            return (id & ((1L << offset()) - 1)) == suffix();
        }
```
这个是为了判断给定的id值判断其最后的offset()位来抉择是不是某种VertexIdType

```
Schema顶点没有partition id
/* ########################################################
               Schema Vertices
   ########################################################  */

    /* --- JanusGraphRelation Type id bit format ---
      *  [ 0 | count | ID padding ]
      *  (there is no partition)
     */
```
## 分析VertexLabel创建流程
会调用JanusGraphSchemaVertex makeSchemaVertex(JanusGraphSchemaCategory schemaCategory, String name, TypeDefinitionMap definition)

![](_v_images/_1517039739_28204.png)

![](_v_images/_1517039789_6737.png)
假设一开始id得到的是1
return (count << offset()) | suffix();
因为一开始是为了创建通用的顶点GenericSchemaType， 所以offset（）是4， suffix()是9 1001b, 最终得到的值是25, 1000o b + 10001 b = 11001b = 25
```
private static long makeTemporary(long id) { //预留bit位
        Preconditions.checkArgument(id>0);
        return (1L <<63) | id; //make negative but preserve bit pattern
    }
```
之后执行了上面的函数， 为了拼接得到1 0000 .... 00011001 b =  -9223372036854775783
![](_v_images/_1517040961_14265.png)
后面是调用assignId(InternalElement element, IDManager.VertexIDType vertexIDType) 先是把vertexLabel当做是NormalVertex
![](_v_images/_1517086819_2605.png)
它是Schema 顶点， 所以其partitionID 为0
当执行完assignId后， 针对VertexLabel就会执行addProperty(schemaVertex, BaseKey.SchemaName, schemaCategory.getSchemaName(name));
BaseKey.SchemaName就是~T$SchemaName， chemaCategory.getSchemaName(name)就是加上前缀vl
然后执行addProperty(schemaVertex, BaseKey.VertexExists, Boolean.TRUE); BaseKey.VertexExists就是~T$VertexExists
addProperty(schemaVertex, BaseKey.SchemaCategory, schemaCategory);BaseKey.SchemaCategory ~T$SchemaCategory
addProperty(schemaVertex, BaseKey.SchemaUpdateTime, times.getTime(times.getTime())); ~T$SchemaUpdateTimestamp

addProperty 会建立StandardVertexProperty 来将vertex 和 系统属性关联上， 比如~T$SchemaName，~T$VertexExists， ~T$SchemaCategory， ~T$SchemaUpdateTimestamp它们都是系统属性节点， 将VertexLabelVertex顶点与系统属性顶点关联起来。
![](_v_images/_1517089677_31691.png)
后面会调用assginId(element, null)来给StandardVertexProperty 赋id值， 先执行下面的代码获取partitionId的值. 如果vertex是Schema的，则partitionId就是0;

![](_v_images/_1517090119_28676.png)

针对StandardVertexProperty 它不是一般的Vertex而是Element是InternalRelation， 所以它会执行else分支， 而不是JanusGraphSchemaVertex 和 PartitionedVertex分支
![](_v_images/_1517098107_5393.png)
上面那个就是一开始要创建PartitionIDPool, 然后长成这样
![](_v_images/_1517099527_18928.png)

nextBlock = idBlockFuture.get(renewTimeout.toMillis(), TimeUnit.MILLISECONDS); 这个函数用于获取IDBlock, 
![](_v_images/_1517099933_944.png)
![](_v_images/_1517099997_16391.png)
最终根据上面获取到count的值 count = idPool.nextID(); 比如此时InternalRelation获取到的是16
然后就开始执行：
![](_v_images/_1517100261_13863.png)
算出来InternalRelation的elementId 是512， 这个就是那个StandardVertexProperty  vp[~T$SchemaName->vlperson]的elementId
connectRelation(prop); 来完成关联动作,将person这个VertexLabelVertex 与 StandardVertexProperty vp[~T$SchemaName->vlperson] 关联起来

关于StandardVertexProperty， 它是没有VertexIDType的

# 流程大纲
创建VertexLabelVertex时，会关联一些系统属性（~T$SchemaName，~T$VertexExists， ~T$SchemaCategory， ~T$SchemaUpdateTimestamp它们都是系统属性节点）到其上面。 然后创建用户点顶点时，会关联创建好的VertexLabelVertex， 同时还会创建比如name, age等PropertyKeyVertex, 让后将PropertyKeyVertex和点顶点继续关联起来。
# VertexIDAssigner
```
int partitionBits = NumberUtil.getPowerOf2(config.get(CLUSTER_MAX_PARTITIONS));//partitionBits 可以通过配置max-partitions进行设置, 默认值是32
```
里面有两个IDPool : schemaIdPool 和 partitionVertexIdPool


# StandardIDPool
standardIDPool是专门用来生成id的

# StandardJanusGraphTx

每次开启一个事物，都会创建一个temporaryIds， 本质就是一个自增的id序列， 供内部使用。在哪使用呢？
比如创建StandardVertex时， 创建StandardVertexProperty时
```
（1）StandardVertex vertex = new StandardVertex(this, IDManager.getTemporaryVertexID(IDManager.VertexIDType.NormalVertex, temporaryIds.nextID()), ElementLifeCycle.New);

（2）StandardVertexProperty prop = new StandardVertexProperty(IDManager.getTemporaryRelationID(temporaryIds.nextID()), key, (InternalVertex) vertex, normalizedValue, ElementLifeCycle.New);
```

```
temporaryIds = new IDPool() {

            private final AtomicLong counter = new AtomicLong(1);

            @Override
            public long nextID() {
                return counter.getAndIncrement();
            }

            @Override
            public void close() {
                //Do nothing
            }
        };
```