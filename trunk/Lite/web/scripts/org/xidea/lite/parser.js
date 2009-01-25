/*
 * JavaScript Integration Framework
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/jsi/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//parse
//var EL_TYPE = 0;
//var VAR_TYPE = 1;//":set"://var
//var IF_TYPE = 2;//":if":
//var ELSE_TYPE = 3;//":else":
//var FOR_TYPE = 4;//":for":
//
//var EL_XML_TEXT_TYPE = 6;
//var ATTRIBUTE_TYPE = 7;//":attribute":
var EL_TYPE = 0;// [0,'el']
var VAR_TYPE = 1;// [1,'value','name']
var IF_TYPE = 2;// [2,[...],'test']
var ELSE_TYPE = 3;// [3,[...],'test']//test opt?
var FOR_TYPE = 4;// [4,[...],'var','items','status']//status
var BREAK_TYPE = 5;// [1,depth]

var EL_XML_TEXT_TYPE = 6;// [6,'el']
var ATTRIBUTE_TYPE = 7;// [7,'value','name']
var ATTRIBUTE_VALUE_TYPE = 8;// [8,'value']//js 实现中忽略该设置，与 EL_XML_TEXT_TYPE,行为一致
var CAPTRUE_TYPE = 9;// [1,[...],'var']

var IF_KEY = "if";
var FOR_KEY = "for";

var SAFE_FOR_KEY = "this['for']";
var TEMPLATE_NS_REG = /^http:\/\/www.xidea.org\/ns\/template.*/;


//add as default
function Parser(){
    this.parserList = this.parserList.concat([]);
    this.result = [];
}


/**
 * @private
 */
Parser.prototype = {
    //nativeJS:false,
    parserList : [],
    /**
     * 添加新解析函数
     * @public
     */
    addParser : function(){
        this.parserList.push.apply(this.parserList,arguments)
    },
    /**
     * 想当前栈顶添加数据
     * 解析和编译过程中使用
     * @public
     */
    append  :  function(){
        var result = this.result;
        for(var i = 0;i<arguments.length;i++){
            var item = arguments[i];
            //alert(result)
            if(result.length){
                if(item.constructor == String){
                    var previous = result.pop();
                    if(previous.constructor == String){
                        result.push(previous+item);
                    }else{
                        result.push(previous,item);  
                    }
                }else{
                    result.push(item);
                }
            }else{
                result.push(item);
            }
        }
        //alert(result)
    },
    /**
     * 移除结尾数据直到上一个end为止（包括该end标记）
     * @public
     */
    removeLastEnd:function(){
        var result = this.result;
        var item;
        while((item = result.pop())!=null){
            if(item instanceof Array && item.length ==0){//end
                break;
            }
        }
    },
    /**
     * 给出文件内容或url，解析模版源文件。
     * 如果指定了base，当作url解析，无base，当作纯文本解析
     * @public
     * @abstract
     * @return <Array> result
     */
    parse : function(node){
        throw new Error("未实现")
    },
    /**
     * 解析源文件文档节点。
     * @public 
     */
    parseNode : function(node){
        var parserList = this.parserList;
        var i = parserList.length;
        while(i-- && node!=null){
            node = parserList[i].call(this,node)
        }
    },
    buildResult:function(){
        if(this.nativeJS){
            return buildNativeJS(buildTreeResult(this.result));
        }else{
            return buildTreeResult(this.result);
        }
    }
}
function buildTreeResult(result){
	var stack = [];//new ArrayList<ArrayList<Object>>();
	var current = [];// new ArrayList<Object>();
	stack.push(current);
	for (var i = 0;i<result.length;i++) {
	    var item = result[i];
		if (item.constructor == String) {
			current.push(item);
		} else {
			if (item.length == 0) {
				var children = stack.pop();
				current = stack[stack.length-1];
				current[current.length - 1][1]=children;
			} else {
				var type = item[0];
				if(type == ELSE_TYPE){
					var children = stack.pop();
					current = stack[stack.length-1];
					current[current.length - 1][1]=children;
				}
				var cmd2 =[];
				cmd2.push(item[0]);
				current.push(cmd2);
				switch (type) {
				case CAPTRUE_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
					cmd2.push(null);
					stack.push(current = []);
				}
				for (var j = 1; j < item.length; j++) {
					cmd2.push(item[j]);
				}

			}
		}
	}
	return current;
}