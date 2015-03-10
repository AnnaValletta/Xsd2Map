import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Xsd2Map {
    private Map map;

    // for parsing date
    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
        put("^\\d{8}$", "yyyyMMdd");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
        put("^\\d{1,2}/[A-Za-z]{3}/\\d{4}$", "dd/MMM/yyyy");
        put("^\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}$", "dd MMM yyyy");
        put("^\\d{1,2}\\s[A-Za-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
        put("^\\d{12}$", "yyyyMMddHHmm");
        put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
        put("^\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
        put("^\\d{1,2}\\s[A-Za-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
        put("^\\d{14}$", "yyyyMMddHHmmss");
        put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
        put("^\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
        put("^\\d{1,2}\\s[A-Za-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}z[+-]\\d{2}:\\d{2}$", "yyyy-MM-dd'T'HH:mm:ssZ");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}[+-]\\d{4}$", "yyyy-MM-dd'T'HH:mm:ssZ");
    }};

    public Map getJsonMap() {
        return map;
    }

    public static Map<String, Object> convertXmlToEsperMap(String xml, final Map<String, String> xsdTypeMap, HashSet<String> arraySet) throws ParseException {
        JSONObject jsonObject = XML.toJSONObject(xml);
        Map map = jsonObject.getJsonMap();
        return convertJsonMapToEsperMap(map, xsdTypeMap, arraySet);
    }

    public static Map<String, Object> convertJsonToEsperMap(String json, final Map<String, String> xsdTypeMap, HashSet<String> arraySet) throws ParseException {
        JSONObject jsonObject = new JSONObject(json);
        Map map = jsonObject.getJsonMap();
        return convertJsonMapToEsperMap(map, xsdTypeMap, arraySet);
    }

    public static Map convertJsonMapToEsperMap(Map<String, Object> jsonMap, final Map<String, String> xsdTypeMap, HashSet<String> arraySet) throws ParseException {
        // a XML complex type is a JSONObject
        // the root element has to be a XML complex type, and therefore the jsonMap will only have one map entry, which is the root element
        Map.Entry<String, Object> rootEntry = jsonMap.entrySet().iterator().next();
        JSONObject root = (JSONObject) rootEntry.getValue();
        return convertJsonMapToEsperMap(root, xsdTypeMap, arraySet, rootEntry.getKey());
    }

    private static Map convertJsonMapToEsperMap(JSONObject jsonObject, final Map<String, String> xsdTypeMap, HashSet<String> arraySet, String currentPropertyPath) throws ParseException {
        HashMap<String, Object> esperMap = new HashMap<String, Object>();

        // now get the map for the real class members
        Iterator it = jsonObject.getJsonMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> item = (Map.Entry<String, Object>) it.next();
            String propertyName = item.getKey();
            // if it is JSONObject, recursively call this method
            // if it is JSONArray, parse the elements and if the element is JSONObject, recursively call this method, we also need another recursive method to call for JSONArray
            // else, read the data type from the xsdTypeMap
            Object value = item.getValue();
            if (value instanceof JSONObject)
                if (currentPropertyPath == null || currentPropertyPath.isEmpty())
                    esperMap.put(propertyName, convertJsonMapToEsperMap((JSONObject)value, xsdTypeMap, arraySet, propertyName));
                else {
                    if (arraySet.contains(currentPropertyPath + "." + propertyName)) {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(value);
                        esperMap.put(propertyName, convertJsonMapToEsperMap(jsonArray, xsdTypeMap, arraySet, currentPropertyPath + "." + propertyName));
                    }
                    else
                        esperMap.put(propertyName, convertJsonMapToEsperMap((JSONObject)value, xsdTypeMap, arraySet, currentPropertyPath + "." + propertyName));
                }
            else if (value instanceof JSONArray)
                if (currentPropertyPath == null || currentPropertyPath.isEmpty())
                    esperMap.put(propertyName, convertJsonMapToEsperMap((JSONArray)value, xsdTypeMap, arraySet, propertyName));
                else {
                    esperMap.put(propertyName, convertJsonMapToEsperMap((JSONArray)value, xsdTypeMap, arraySet, currentPropertyPath + "." + propertyName));
                }
            else { // it must be primitive type if it is not JSONObject or JSONArray
                String currentFullPropertyName = currentPropertyPath.isEmpty() ? propertyName : currentPropertyPath + "." + propertyName;
                String dataType = xsdTypeMap.get(currentFullPropertyName);
//                System.out.println(String.format("DataType-%s,currentFullPropertyName-%s,propertyName-%s,value-%s,JsonType-%s", dataType, currentFullPropertyName, propertyName, value.toString(), value.getClass().getName()));
                esperMap.put(propertyName, convertToValueObject(dataType, value));
            }
        }

        return esperMap;
    }

    private static Object[] convertJsonMapToEsperMap(JSONArray jsonArray, final Map<String, String> xsdTypeMap, HashSet<String> arraySet, String currentPropertyPath) throws ParseException {
        // for a JSONArray, the currentPropertyPath should not be null
        // JSONArray should be of the same type, and it should not be JSONArray. Let's probe the type.
        Object firstItem = jsonArray.getMyArrayList().get(0);
        if (firstItem instanceof JSONObject) {
            Map[] esperMaps = new HashMap[jsonArray.length()];

            ArrayList arrayList = jsonArray.getMyArrayList();
            for (int i = 0; i < arrayList.size(); i++) {
                Object item = arrayList.get(i);
                // if it is JSONObject, recursively call the JSONObject convert method
                esperMaps[i] = convertJsonMapToEsperMap((JSONObject) item, xsdTypeMap, arraySet, currentPropertyPath);
            }
            return esperMaps;
        } else { // it must be primitive type, read the data type from the xsdTypeMap
            Object[] primitives = new Object[jsonArray.length()];
            ArrayList arrayList = jsonArray.getMyArrayList();
            String dataType = xsdTypeMap.get(currentPropertyPath);
            for (int i = 0; i < arrayList.size(); i++) {
//                System.out.println(String.format("DataType-%s,currentFullPropertyName-%s,value-%s,JsonType-%s", dataType, currentPropertyPath, arrayList.get(i).toString(), arrayList.get(i).getClass().getName()));
                primitives[i] = convertToValueObject(dataType, arrayList.get(i));
            }
            return primitives;
        }
    }

    private static Object convertToValueObject(String dataType, Object value) throws ParseException {
        if (value == null) return null;
        if (dataType == null || dataType.isEmpty()) return null;

        if (dataType.equalsIgnoreCase("boolean"))
            // for "boolean", value is a "Boolean" from the jsonobject
            return (Boolean) value;
        if (dataType.equalsIgnoreCase("string"))
            return value.toString();
        if (dataType.equalsIgnoreCase("float"))
            // for "float", value is a "Double" from the jsonobject
            return ((Float)((Double)value).floatValue());
        if (dataType.equalsIgnoreCase("double"))
            // for "double", value is already a "Double" from the jsonobject
            return value;
        if (dataType.equalsIgnoreCase("decimal"))
            // for "decimal", value is a "string" from the jsonobject
            return new BigDecimal(value.toString());
        if (dataType.equalsIgnoreCase("datetime") || dataType.equalsIgnoreCase("date")) {
            // for "datetime", value is a "string" from the jsonobject
            String dateFormat = determineDateFormat(value.toString());
            if (dateFormat != null) {
                return new SimpleDateFormat(dateFormat).parse(value.toString());
            }
        }
        if (dataType.equalsIgnoreCase("integer") || dataType.equalsIgnoreCase("nonnegativeinteger") || dataType.equalsIgnoreCase("nonpositiveinteger")
                || dataType.equalsIgnoreCase("negativeinteger") || dataType.equalsIgnoreCase("positiveinteger") || dataType.equalsIgnoreCase("unsignedlong"))
            // for "integer", value is a "Integer" or "Long" from the jsonobject
            if (value instanceof Integer) {
                return BigInteger.valueOf(new Long(((Integer) value).intValue()));
            } else {
                return BigInteger.valueOf((Long)value);
            }
        if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("unsignedshort") || dataType.equalsIgnoreCase("byte"))
            // for "int", value is a "Integer" from the jsonobject
            return value;
        if (dataType.equalsIgnoreCase("short") || dataType.equalsIgnoreCase("unsignedbyte"))
            // for "short", valule is a "Integer" from the jsonobject
            return new Short((short)((Integer)value).intValue());
        if (dataType.equalsIgnoreCase("long") || dataType.equalsIgnoreCase("unsignedint")) {
            // for "long", value is a "Long" or "Integer" from the jsonobject
            if (value instanceof Integer) {
                return new Long(((Integer)value).intValue());
            } else {
                return value;
            }
        }
        if (dataType.equalsIgnoreCase("character"))
            // for "character", value is a "string" from the jsonobject
            return new Character(value.toString().charAt(0));
        if (dataType.equalsIgnoreCase("hexbinary"))
            return hexStringToByteArray(value.toString());
//        if (dataType.equalsIgnoreCase("base64binary"))
//            return Base64.decodeBase64(value.toString());

        // everything else returns a string
        return value.toString();
//        throw new ParseException("error converting to " + dataType + ", value is " + value.toString(), 0);
    }

    private static Object getJavaTypeFromXmlString(String dataType) {
        if (dataType == null || dataType.isEmpty()) return null;

        if (dataType.equalsIgnoreCase("boolean"))
            // for "boolean", value is a "Boolean" from the jsonobject
            return Boolean.class;
        if (dataType.equalsIgnoreCase("boolean[]"))
            return Boolean[].class;
        if (dataType.equalsIgnoreCase("string"))
            return String.class;
        if (dataType.equalsIgnoreCase("string[]"))
            return String[].class;
        if (dataType.equalsIgnoreCase("float"))
            // for "float", value is a "Double" from the jsonobject
            return Float.class;
        if (dataType.equalsIgnoreCase("float[]"))
            return Float[].class;
        if (dataType.equalsIgnoreCase("double"))
            // for "double", value is already a "Double" from the jsonobject
            return Double.class;
        if (dataType.equalsIgnoreCase("double[]"))
            return Double[].class;
        if (dataType.equalsIgnoreCase("decimal"))
            // for "decimal", value is a "string" from the jsonobject
            return BigDecimal.class;
        if (dataType.equalsIgnoreCase("decimal[]"))
            return BigDecimal[].class;
        if (dataType.equalsIgnoreCase("datetime") || dataType.equalsIgnoreCase("time") || dataType.equalsIgnoreCase("date")) {
            return Date.class;
        }
        if (dataType.equalsIgnoreCase("datetime[]") || dataType.equalsIgnoreCase("time[]") || dataType.equalsIgnoreCase("date[]")) {
            return Date[].class;
        }
        if (dataType.equalsIgnoreCase("integer") || dataType.equalsIgnoreCase("nonnegativeinteger") || dataType.equalsIgnoreCase("nonpositiveinteger")
                || dataType.equalsIgnoreCase("negativeinteger") || dataType.equalsIgnoreCase("positiveinteger") || dataType.equalsIgnoreCase("unsignedlong"))
            // for "integer", value is a "Integer" from the jsonobject
            return BigInteger.class;
        if (dataType.equalsIgnoreCase("integer[]") || dataType.equalsIgnoreCase("nonnegativeinteger[]") || dataType.equalsIgnoreCase("nonpositiveinteger[]")
                || dataType.equalsIgnoreCase("negativeinteger[]") || dataType.equalsIgnoreCase("positiveinteger[]") || dataType.equalsIgnoreCase("unsignedlong[]"))
            return BigInteger[].class;
        if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("unsignedshort") || dataType.equalsIgnoreCase("byte"))
            // for "int", value is a "Integer" from the jsonobject
            return Integer.class;
        if (dataType.equalsIgnoreCase("int[]") || dataType.equalsIgnoreCase("unsignedshort[]") || dataType.equalsIgnoreCase("byte[]"))
            return Integer[].class;
        if (dataType.equalsIgnoreCase("short") || dataType.equalsIgnoreCase("unsignedbyte"))
            // for "short", valule is a "Integer" from the jsonobject
            return Short.class;
        if (dataType.equalsIgnoreCase("short[]") || dataType.equalsIgnoreCase("unsignedbyte[]"))
            return Short[].class;
        if (dataType.equalsIgnoreCase("long") || dataType.equalsIgnoreCase("unsignedint"))
            // for "long", value is a "Long" from the jsonobject
            return Long.class;
        if (dataType.equalsIgnoreCase("long[]") || dataType.equalsIgnoreCase("unsignedint[]"))
            return Long[].class;
        if (dataType.equalsIgnoreCase("character"))
            // for "character", value is a "string" from the jsonobject
            return Character.class;
        if (dataType.equalsIgnoreCase("character[]"))
            return Character[].class;
        if (dataType.equalsIgnoreCase("hexbinary"))
            return (new byte[0]).getClass();
//        if (dataType.equalsIgnoreCase("base64binary"))
//            return Base64.decodeBase64(value.toString());

        // everything else should be a complex type, just return it
        return dataType;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DATE_FORMAT_REGEXPS.get(regexp);
            }
        }
        return null; // Unknown format.
    }

    public static List<Pair> convertXsdToEsperDefinitionMap(Map<String, String> xsdTypeMap, HashSet<String> arrayElementSet, String esperAlias) {
        // a linked list of maps of esper-ready data types
        if (xsdTypeMap == null || xsdTypeMap.size() == 0)
            return new LinkedList<Pair>();

        Tree tree = new Tree();
        // turn the xsd type map into a tree
        for (Map.Entry<String, String> entry : xsdTypeMap.entrySet()) {
            tree.createNodes(entry.getKey(), entry.getValue());
        }

        return tree.traverse(esperAlias, arrayElementSet);
    }


    public static class Pair {
        String name;
        Map<String, Object> map;

        public Pair(String name, Map<String, Object> map) {
            this.name = name;
            this.map = map;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getMap() {
            return map;
        }
    }

    private static class Node {
        private String name;
        private String type;
        private Node parent;
        private List<Node> children;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        private Node(String name, String type, Node parent) {
            this.name = name;
            this.type = type;
            this.parent = parent;
            children = new ArrayList<Node>();
        }

        // create the child node if it does not exist, or get the existing one if there is one already
        public Node createChildNode(String name, String type) {
            Node child = childAlreadyExists(name);
            if (child == null) {
                child = new Node(name, type, this);
                children.add(child);
                return child;
            } else { // update node with type if type is empty
                if (child.type.isEmpty())
                    child.type = type;
                return child;
            }
        }

        private Node childAlreadyExists(String childName) {
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i).getName().equalsIgnoreCase(childName))
                    return children.get(i);
            }
            return null;
        }
    }

    private static class Tree {
        private Node root;

        public Tree() {
        }

//        public Tree(String rootData, String type) {
//            root = new Node(rootData, type, null);
//            root.name = rootData;
//            root.type = type;
//            root.parent = null;
//            root.children = new ArrayList<Node>();
//        }

        // the path should be a full path (always starts from the root node)
        public void createNodes(String path, String type) {
            if (path == null || path.isEmpty())
                return;

            String[] elements = path.split("\\.");

            if (root == null) { // create root if root does not exist yet
                if (elements.length == 1)
                    root = new Node(elements[0], type, null);
                else
                    root = new Node(elements[0], "", null);
            } else {
                // update the root with the correct type
                if (elements.length == 1)
                    root.type = type;
            }

            Node currentNode = root;
            for (int i = 1; i < elements.length; i++) { // we start from [1], because [0] is always the root node
                if (i == elements.length - 1) // for the last element, it has the type information
                    currentNode = currentNode.createChildNode(elements[i], type);
                else
                    currentNode = currentNode.createChildNode(elements[i], "");
            }
        }

        // traverse the tree breadth first, and insert the node (addFirst) one by one,
        // this will ensure all the independent data type occurs first
        public List<Pair> traverse(String rootLabel, HashSet<String> arraySet) {
            LinkedList<Pair> pairList = new LinkedList<Pair>();

            // a node list for tree traversal
            List<Node> nodeList = new ArrayList<Node>();
            nodeList.add(root);
            Node currentNode;
            for (int i = 0; i < nodeList.size(); i++) {
                currentNode = nodeList.get(i);
                if (currentNode != null && currentNode.children.size() != 0) { // if the node has children, then it is a complex type
                    Map<String, Object> map = new HashMap<String, Object>();
                    for (Node child : currentNode.children) {
                        nodeList.add(child);
                        Object javaType;
                        if (arraySet != null && arraySet.contains(getCurrentPath(child))) { // this node is an array
                            // check if the node (child node now) is an array of primitive type
                            javaType = getJavaTypeFromXmlString(child.getType() + "[]");
                            if (javaType instanceof String)
                                javaType = rootLabel + "." + javaType;
                        }
                        else {
                            javaType = getJavaTypeFromXmlString(child.getType());
                            if (javaType instanceof String)
                                // instanceof String means this is a complex type
                                javaType = rootLabel + "." + javaType;
                        }
                        map.put(child.getName(), javaType);
                    }
                    pairList.addFirst(new Pair(rootLabel + "." + currentNode.getType(), map));
                }
            }

            return pairList;
        }

        public String getCurrentPath(Node node) {
            Node parent = node.parent;
            if (parent == null)
                return node.getName();
            else {
                StringBuilder sb = new StringBuilder();
                sb.append(node.getName());
                while (parent != null) {
                    sb.insert(0, parent.getName() + ".");
                    parent = parent.parent;
                }
                return sb.toString();
            }
        }
    }
}
