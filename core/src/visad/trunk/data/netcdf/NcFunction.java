package visad.data.netcdf;


import java.io.IOException;
import visad.CoordinateSystem;
import visad.DataImpl;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.GriddedSet;
import visad.IntegerNDSet;
import visad.IntegerSet;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Linear3DSet;
import visad.LinearNDSet;
import visad.LinearSet;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.TextType;
import visad.TupleType;
import visad.UnimplementedException;
import visad.Unit;
import visad.VisADException;


/**
 * Class for adapting a netCDF function to a VisAD function.
 */
class
NcFunction
    extends	NcData
{
    /**
     * The netCDF dimensions of the function domain IN VisAD ORDER:
     */
    protected /*final*/ NcDim[]		dims;

    /**
     * The netCDF variables of the function range:
     */
    protected /*final*/ ImportVar[]	vars;

    /**
     * Whether or not there's a textual component in the range.
     */
    protected boolean			hasTextualComponent;


    /**
     * Protected default constructor.
     */
    protected
    NcFunction()
    {
    }


    /**
     * Construct from an array of adapted, netCDF variables.
     *
     * @precondition	All variables have the same (ordered) set of dimensions.
     * @exception UnimplementedException	Not yet!
     * @exception VisADException		Couldn't create necessary 
     *						VisAD object.
     * @exception IOException			I/O error.
     */
    NcFunction(ImportVar[] vars)
	throws UnimplementedException, VisADException, IOException
    {
	initialize(vars[0].getDimensions(), vars);
    }


    /**
     * Protected initializer for derived classes.
     *
     * @precondition	All variables have the same (ordered) set of dimensions.
     * @exception UnimplementedException	Not yet!
     * @exception VisADException		Couldn't create necessary 
     *						VisAD object.
     * @exception IOException			I/O error.
     */
    protected void
    initialize(NcDim[] varDims, ImportVar[] vars)
	throws UnimplementedException, VisADException, IOException
    {
	this.vars = vars;

	/*
	 * Because the VisAD and netCDF dimensional orderings are opposite,
	 * we invert the sequence of dimensions.
	 */
	{
	    int		rank = varDims.length;

	    this.dims = new NcDim[rank];

	    for (int idim = 0; idim < rank; ++idim)
		this.dims[idim] = varDims[rank-1-idim];
	}

	initialize(new FunctionType(getDomainMathType(), getRangeMathType()));
    }


    /**
     * Return the VisAD MathType of the domain.
     *
     * @exception VisADException	Couldn't create necessary VisAD object.
     */
    protected MathType
    getDomainMathType()
	throws VisADException
    {
	int		rank = dims.length;
	MathType	type;

	if (rank == 1)
	    type = dims[0].getMathType();
	else
	{
	    RealType[]	types = new RealType[rank];

	    for (int idim = 0; idim < rank; ++idim)
		types[idim] = dims[idim].getMathType();

	    type = new RealTupleType(types);
	}

	return type;
    }


    /**
     * Return the VisAD MathType of the range.
     *
     * @exception VisADException	Couldn't create necessary VisAD object.
     */
    protected MathType
    getRangeMathType()
	throws VisADException
    {
	int		nvars = vars.length;
	MathType	type;

	hasTextualComponent = false;

	if (nvars == 0)
	    type = null;
	else
	if (nvars == 1)
	{
	    type = vars[0].getMathType();
	    if (type instanceof TextType)
		hasTextualComponent = true;
	}
	else
	{
	    for (int i = 0; i < nvars; ++i)
		if (vars[i].getMathType() instanceof TextType)
		{
		    hasTextualComponent = true;
		    break;
		}

	    if (hasTextualComponent)
	    {
		MathType[]	types = new MathType[nvars];

		for (int i = 0; i < nvars; ++i)
		    types[i] = vars[i].getMathType();

		type = new TupleType(types);
	    }
	    else
	    {
		RealType[]	types = new RealType[nvars];

		for (int i = 0; i < nvars; ++i)
		    types[i] = (RealType)vars[i].getMathType();

		type = new RealTupleType(types);
	    }
	}

	return type;
    }


    /**
     * Return the VisAD data object corresponding to this function.
     */
    DataImpl
    getData()
	throws IOException, VisADException
    {
	FieldImpl	field;

	if (hasTextualComponent)
	{
	    // TODO
	    field = null;
	}
	else
	{
	    FlatField	flatField =
		new FlatField((FunctionType)mathType, getDomainSet(),
		    (CoordinateSystem)null, getRangeSets(), getRangeUnits());

	    flatField.setSamples(getRangeDoubles(), /*copy=*/false);

	    field = flatField;
	}

	return field;
    }


    /**
     * Return the domain-set of this function.
     */
    protected Set
    getDomainSet()
	throws IOException, VisADException
    {
	Set		domainSet;
	int		rank = dims.length;
	ImportVar[]	coordVars = new ImportVar[rank];
	boolean		noCoordVars = true;
	boolean	 	allCoordVarsAreArithProgs = true;
	ArithProg[]	aps = new ArithProg[rank];

	/*
	 * Because the domain-set can be any one of several subtypes, 
	 * first determine the appropriate subtype.
	 */
	for (int idim = 0; idim < rank; ++idim)
	{
	    coordVars[idim] = dims[idim].getCoordVar();

	    if (coordVars[idim] != null)
	    {
		noCoordVars = false;

		if (allCoordVarsAreArithProgs)
		{
		    if (coordVars[idim].isLongitude())
			aps[idim] = new LonArithProg();
		    else
			aps[idim] = new ArithProg();

		    if (!aps[idim].accumulate(coordVars[idim].getFloatValues()))
			allCoordVarsAreArithProgs = false;
		}
	    }
	}

	if (noCoordVars)
	{
	    /*
	     * This domain has no co-ordinate variables.
	     */
	    domainSet = getIntegerSet();
	}
	else
	if (allCoordVarsAreArithProgs)
	{
	    /*
	     * This domain has co-ordinate variables -- all of which are
	     * arithmetic progressions.
	     */
	    domainSet = (Set)getLinearSet(aps, coordVars);
	}
	else
	{
	    /*
	     * This domain has at least one co-ordinate variable which is 
	     * not an arithmetic progression.  This is the general case.
	     */
	    domainSet = getGriddedSet(coordVars);
	}

	return domainSet;
    }


    /**
     * Return the IntegerSet of this function.
     *
     * @precondition	The domain-set of this function is (logically)
     *			an IntegerSet.
     */
    protected GriddedSet
    getIntegerSet()
	throws VisADException
    {
	int	rank = dims.length;
	int[]	lengths = new int[rank];

	for (int idim = 0; idim < rank; ++idim)
	    lengths[idim] = dims[idim].getLength();

	// TODO: add CoordinateSystem argument
	return IntegerNDSet.create(
	    ((FunctionType)mathType).getDomain(), lengths);
    }


    /**
     * Return the LinearSet of this function.
     *
     * @precondition	The domain-set of this function is (logically)
     *			a LinearSet.
     */
    protected LinearSet
    getLinearSet(ArithProg[] aps, ImportVar[] coordVars)
	throws VisADException
    {
	int		rank = dims.length;
	double[]	firsts = new double[rank];
	double[]	lasts = new double[rank];
	int[]		lengths = new int[rank];

	for (int idim = 0; idim < rank; ++idim)
	{
	    if (coordVars[idim] == null)
	    {
		/*
		 * The dimension doesn't have a co-ordinate variable.
		 */
		firsts[idim] = 0;
		lengths[idim] = dims[idim].getLength();
		lasts[idim] = lengths[idim] - 1;
	    }
	    else
	    {
		/*
		 * The dimension has a co-ordinate variable.
		 */
		firsts[idim] = aps[idim].getFirst();
		lasts[idim] = aps[idim].getLast();
		lengths[idim] = aps[idim].getNumber();
	    }
	}

	// TODO: add CoordinateSystem argument
	return LinearNDSet.create(((FunctionType)mathType).getDomain(),
				    firsts, lasts, lengths,
				    null, null, null);
    }


    /**
     * Return the GriddedSet of this function.
     *
     * @precondition	The domain-set of this function is a GriddedSet.
     */
    protected GriddedSet
    getGriddedSet(ImportVar[] coordVars)
	throws VisADException, IOException
    {
	int		rank = dims.length;
	int[]		lengths = new int[rank];
	float[][]	values = new float[rank][];
	int		ntotal = 1;

	for (int idim = 0; idim < rank; ++idim)
	{
	    lengths[idim] = dims[idim].getLength();
	    ntotal *= lengths[idim];
	}

	for (int idim = 0; idim < rank; ++idim)
	{
	    float[]	vals;

	    values[idim] = new float[ntotal];

	    if (coordVars[idim] != null)
		vals = coordVars[idim].getFloatValues();
	    else
	    {
		int	npts = lengths[idim];

		vals = new float[npts];

		for (int ipt = 0; ipt < npts; ++ipt)
		    vals[ipt] = ipt;
	    }

	    for (int pos = 0; pos < ntotal/vals.length; pos += vals.length)
		System.arraycopy(vals, 0, values[idim], pos, vals.length);
	}

	// TODO: add CoordinateSystem argument
	return GriddedSet.create(
	    ((FunctionType)mathType).getDomain(), values, lengths);
    }


    /**
     * Return the range Sets of this function.
     */
    protected Set[]
    getRangeSets()
    {
	Set[]	sets = new Set[vars.length];

	for (int i = 0; i < vars.length; ++i)
	    sets[i] = ((NcNumber)vars[i]).getSet();

	return sets;
    }


    /**
     * Return the range Units of this function.
     */
    protected Unit[]
    getRangeUnits()
    {
	Unit[]	units = new Unit[vars.length];

	for (int i = 0; i < vars.length; ++i)
	    units[i] = vars[i].getUnit();

	return units;
    }


    /**
     * Return the range values of this function as doubles.
     */
    protected double[][]
    getRangeDoubles()
	throws VisADException, IOException
    {
	int		nvars = vars.length;
	double[][]	values = new double[nvars][];

	for (int ivar = 0; ivar < nvars; ++ivar)
	    values[ivar] = vars[ivar].getDoubleValues();

	return values;
    }
}


