package ModelInterface;

public class UnitConversionInstance {
	private String toUnit;
	private String fromUnit;
	private Double conversionFactor;
	private String query;
	private String headingOne;
	private String valueOne;
	private String headingTwo;
	private String valueTwo;
	private String headingThree;
	private String valueThree;
	private String headingFour;
	private String valueFour;
	
	public UnitConversionInstance(String fromUnit, String toUnit, Double conversionFactor, String query, String headingOne,
			String valueOne, String headingTwo, String valueTwo, String headingThree, String valueThree,
			String headingFour, String valueFour) {
		super();
		this.toUnit = toUnit;
		this.fromUnit=fromUnit;
		this.conversionFactor = conversionFactor;
		this.query = query;
		this.headingOne = headingOne;
		this.valueOne = valueOne;
		this.headingTwo = headingTwo;
		this.valueTwo = valueTwo;
		this.headingThree = headingThree;
		this.valueThree = valueThree;
		this.headingFour = headingFour;
		this.valueFour = valueFour;
	}

	public String getToUnit() {
		return toUnit;
	}
	
	public String getFromUnit() {
		return fromUnit;
	}

	public Double getConversionFactor() {
		return conversionFactor;
	}

	public String getQuery() {
		return query;
	}

	public String getHeadingOne() {
		return headingOne;
	}

	public String getValueOne() {
		return valueOne;
	}

	public String getHeadingTwo() {
		return headingTwo;
	}

	public String getValueTwo() {
		return valueTwo;
	}

	public String getHeadingThree() {
		return headingThree;
	}

	public String getValueThree() {
		return valueThree;
	}

	public String getHeadingFour() {
		return headingFour;
	}

	public String getValueFour() {
		return valueFour;
	}
	
	
	

}
