import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Xsd2MapTest extends TestCase {
    String sAllDataTypesXsd = "<?xml version=\"1.0\"?>\n" +
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  <xs:element name=\"AllDataTypes\" type=\"AllDataTypes\"/>\n" +
            "  <xs:complexType name=\"AllDataTypes\">\n" +
            "    <xs:sequence>\n" +
            "      <xs:element type=\"xs:string\" name=\"msgType\"/>\n" +
            "      <xs:element type=\"xs:int\" name=\"XsdCheckerDelay\"/>\n" +
            "      <xs:element type=\"xs:dateTime\" name=\"XsdCheckerExpiry\"/>\n" +
            "      <xs:element type=\"xs:float\" name=\"price\"/>\n" +
            "      <xs:element type=\"xs:long\" name=\"amount\"/>\n" +
            "      <xs:element type=\"xs:boolean\" name=\"isExternal\"/>\n" +
            "      <xs:element type=\"xs:double\" name=\"doubleValue\"/>\n" +
            "      <xs:element type=\"xs:decimal\" name=\"decimalValue\"/>\n" +
            "      <xs:element type=\"xs:date\" name=\"dateValue\"/>\n" +
            "      <xs:element type=\"xs:integer\" name=\"integerValue\"/>\n" +
            "      <xs:element type=\"xs:unsignedLong\" name=\"unsignedLongValue\"/>\n" +
            "      <xs:element type=\"xs:unsignedShort\" name=\"unsignedShortValue\"/>\n" +
            "      <xs:element type=\"xs:byte\" name=\"byteValue\"/>\n" +
            "      <xs:element type=\"xs:short\" name=\"shortValue\"/>\n" +
            "      <xs:element type=\"xs:unsignedByte\" name=\"unsignedByteValue\"/>\n" +
            "      <xs:element type=\"xs:unsignedInt\" name=\"unsignedIntValue\"/>\n" +
//            "      <xs:element type=\"xs:character\" name=\"characterValue\"/>\n" +
            "      <xs:element type=\"xs:nonNegativeInteger\" name=\"nonNegativeIntegerValue\"/>\n" +
            "      <xs:element type=\"xs:nonPositiveInteger\" name=\"nonPositiveIntegerValue\"/>\n" +
            "      <xs:element type=\"xs:negativeInteger\" name=\"negativeIntegerValue\"/>\n" +
            "      <xs:element type=\"xs:positiveInteger\" name=\"positiveIntegerValue\"/>\n" +
            "      <xs:element type=\"xs:hexBinary\" name=\"hexBinaryValue\"/>\n" +
            "    </xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "</xs:schema>\n";

    String sAllDataTypes = "{\n" +
            "\t\"AllDataTypes\": {\n" +
            "\t  \"msgType\": \"AllDataTypes_MsgType\",\n" +
            "\t  \"XsdCheckerDelay\": 60000,\n" +
            "\t  \"XsdCheckerExpiry\": \"2014-09-04T20:04:53+0100\",\n" +
            "\t  \"price\": 0.62000,\n" +
            "\t  \"amount\": 10000000,\n" +
            "\t  \"isExternal\": false,\n" +
            "\t  \"doubleValue\": 123.456789,\n" +
            "\t  \"decimalValue\": 112233447788,\n" +
            "\t  \"dateValue\": \"2014-09-04\",\n" +
            "\t  \"integerValue\": 123456,\n" +
            "\t  \"unsignedLongValue\": 11223344556677,\n" +
            "\t  \"unsignedShortValue\": 22334,\n" +
            "\t  \"byteValue\": -23,\n" +
            "\t  \"shortValue\": 11223,\n" +
            "\t  \"unsignedByteValue\": 124,\n" +
            "\t  \"unsignedIntValue\": 223355,\n" +
//            "\t  \"characterValue\": 'c',\n" +
            "\t  \"nonNegativeIntegerValue\": 223445,\n" +
            "\t  \"nonPositiveIntegerValue\": -223445,\n" +
            "\t  \"negativeIntegerValue\": -223444,\n" +
            "\t  \"positiveIntegerValue\": 223444,\n" +
            "\t  \"hexBinaryValue\": FFCC9E\n" +
            "\t}\n" +
            "}";

    HashMap<String, String> xsdTypeMapAutonomy;
    HashMap<String, String> xsdTypeMapAbfx;
    HashMap<String, String> xsdTypeMapAspen;
    HashMap<String, String> xsdTypeMapAllDataTypes;
    HashSet<String> arraySet;

    @Override
    protected void setUp() throws Exception {
        XsdChecker checker = new XsdChecker();
        checker.getSchemaInvalidErrors(new StringReader(sAllDataTypesXsd));
        xsdTypeMapAllDataTypes = checker.getXsdTypeMap();
    }

    private void verifyEsperMapForAllDataTypes(Map map) throws ParseException {
        Assert.assertTrue(map.get("XsdCheckerExpiry") instanceof Date);
        Assert.assertEquals(map.get("XsdCheckerExpiry").toString(), "Thu Sep 04 15:04:53 EDT 2014");
        Assert.assertTrue(map.get("unsignedShortValue") instanceof Integer);
        Assert.assertEquals(map.get("unsignedShortValue"), 22334);
        Assert.assertTrue(map.get("unsignedByteValue") instanceof Short);
        Assert.assertEquals(map.get("unsignedByteValue"), Short.valueOf("124"));
        Assert.assertTrue(map.get("doubleValue") instanceof Double);
        Assert.assertEquals(map.get("doubleValue"), 123.456789);
        Assert.assertTrue(map.get("byteValue") instanceof Integer);
        Assert.assertEquals(map.get("byteValue"), -23);
        Assert.assertTrue(map.get("decimalValue") instanceof BigDecimal);
        Assert.assertEquals((BigDecimal)map.get("decimalValue"), BigDecimal.valueOf(112233447788L));
        Assert.assertTrue(map.get("unsignedLongValue") instanceof BigInteger);
        Assert.assertEquals((BigInteger)map.get("unsignedLongValue"), BigInteger.valueOf(11223344556677L));
        Assert.assertTrue(map.get("unsignedIntValue") instanceof Long);
        Assert.assertEquals(map.get("unsignedIntValue"), 223355L);
        Assert.assertTrue(map.get("shortValue") instanceof Short);
        Assert.assertEquals(map.get("shortValue"), Short.valueOf("11223"));
        Assert.assertTrue(map.get("price") instanceof Float);
        Assert.assertEquals(map.get("price"), 0.62f);
        Assert.assertTrue(map.get("XsdCheckerDelay") instanceof Integer);
        Assert.assertEquals(map.get("XsdCheckerDelay"), 60000);
        Assert.assertTrue(map.get("integerValue") instanceof BigInteger);
        Assert.assertEquals(map.get("integerValue"), BigInteger.valueOf(123456L));
        Assert.assertEquals(map.get("nonNegativeIntegerValue"), BigInteger.valueOf(223445L));
        Assert.assertEquals(map.get("nonPositiveIntegerValue"), BigInteger.valueOf(-223445L));
        Assert.assertEquals(map.get("negativeIntegerValue"), BigInteger.valueOf(-223444L));
        Assert.assertEquals(map.get("positiveIntegerValue"), BigInteger.valueOf(223444L));
        Assert.assertTrue(map.get("hexBinaryValue") instanceof byte[]);
        byte[] bs = (byte[]) map.get("hexBinaryValue");
        Assert.assertEquals(bs.length, 3);
        Assert.assertEquals(bs[0], (byte) -1);
        Assert.assertEquals(bs[1], (byte) -52);
        Assert.assertEquals(bs[2], (byte) -98);
        Assert.assertTrue(map.get("msgType") instanceof String);
        Assert.assertEquals(map.get("msgType"), "AllDataTypes_MsgType");
        Assert.assertTrue(map.get("dateValue") instanceof Date);
        Assert.assertEquals(map.get("dateValue").toString(), "Thu Sep 04 00:00:00 EDT 2014");
        Assert.assertTrue(map.get("isExternal") instanceof Boolean);
        Assert.assertEquals(map.get("isExternal"), false);
        Assert.assertTrue(map.get("amount") instanceof Long);
        Assert.assertEquals(map.get("amount"), 10000000L);
    }

    @Test
    public void testAllDataTypes() throws ParseException {
        JSONObject jsonObject = new JSONObject(sAllDataTypes);
        Map map = jsonObject.getJsonMap();
        Map esperMap = (new JSONObject()).convertJsonMapToEsperMap(map, xsdTypeMapAllDataTypes, null);
        verifyEsperMapForAllDataTypes(esperMap);
    }
}