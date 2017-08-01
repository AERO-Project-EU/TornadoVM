/* 
 * Copyright 2012 James Clarkson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tornado.graal.nodes;

import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.nodeinfo.InputType;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.ValueNode;

@NodeInfo(nameTemplate = "Range")
public class ParallelRangeNode extends AbstractParallelNode {
	public static final NodeClass<ParallelRangeNode>	TYPE	= NodeClass
															.create(ParallelRangeNode.class);

	@Input(InputType.Association) private ParallelOffsetNode offset;
	@Input(InputType.Association) private ParallelStrideNode stride;
	
	public ParallelRangeNode(int index, ValueNode range, ParallelOffsetNode offset, ParallelStrideNode stride) {
		super(TYPE,index,range);
		this.offset = offset;
		this.stride = stride;
	}
	
	public ParallelOffsetNode offset(){
		return offset;
	}
	
	public ParallelStrideNode stride(){
		return stride;
	}

}