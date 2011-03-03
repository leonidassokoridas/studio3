/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.json;

/**
 * SchemaArray
 */
public class SchemaArray implements IState
{
	private enum ArrayState
	{
		READY, IN_ARRAY, IN_ELEMENT, COMPLETE
	}

	private Schema _owningSchema;
	private String _elementTypeName;
	private ArrayState _currentState;

	/**
	 * SchemaArray
	 * 
	 * @param elementType
	 */
	public SchemaArray(Schema owningSchema, String elementTypeName)
	{
		this._owningSchema = owningSchema;
		this._elementTypeName = elementTypeName;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.State#enter()
	 */
	public void enter()
	{
		this._currentState = ArrayState.READY;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.State#exit()
	 */
	public void exit()
	{
	}

	/**
	 * getElementType
	 * 
	 * @return
	 */
	public IState getElementType()
	{
		return this._owningSchema.getType(this._elementTypeName);
	}

	/**
	 * getElementTypeName
	 * 
	 * @return
	 */
	public String getElementTypeName()
	{
		return this._elementTypeName;
	}

	/**
	 * getOwningSchema
	 * 
	 * @return
	 */
	public Schema getOwningSchema()
	{
		return this._owningSchema;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.IState#isValidTransition(com.aptana.json.EventType, java.lang.Object)
	 */
	public boolean isValidTransition(SchemaEventType event, Object value)
	{
		boolean result = false;

		switch (event)
		{
			case START_ARRAY:
				result = (this._currentState == ArrayState.READY);
				break;

			case END_ARRAY:
				result = (this._currentState == ArrayState.IN_ARRAY);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.State#transition(com.aptana.json.Context, com.aptana.json.EventType, java.lang.Object)
	 */
	public void transition(ISchemaContext context, SchemaEventType event, Object value)
	{
		switch (event)
		{
			case START_ARRAY:
				if (this._currentState != ArrayState.READY)
				{
					throw new IllegalStateException("Attempted to start an array that has already been started");
				}

				// update internal state
				this._currentState = ArrayState.IN_ARRAY;

				// fire list creation event
				context.createList(this.getElementTypeName(), this.getElementType());

				// fire element type creation event
				// context.createType(this.getElementTypeName(), this.getElementType());

				// lock stack top to leave element type as active type until we leave the array
				// context.saveTop();
				break;

			case START_ARRAY_ENTRY:
				if (this._currentState != ArrayState.IN_ARRAY)
				{
					throw new IllegalStateException("Attempted to start an array element in array that has not been started");
				}

				// update internal state
				this._currentState = ArrayState.IN_ELEMENT;

				// Push element type into current context. Note that processing of that type will automatically remove
				// itself from the stack
				context.pushType(this.getElementTypeName(), this.getElementType());

				// fire element type creation event
				context.createType(this.getElementTypeName(), this.getElementType());
				break;

			case END_ARRAY:
				if (this._currentState != ArrayState.IN_ARRAY)
				{
					throw new IllegalStateException("Attempted to end an array that has not been started");
				}

				// update internal state
				this._currentState = ArrayState.COMPLETE;

				// Remove this type from the current context
				context.popType();
				break;

			case END_ARRAY_ENTRY:
				if (this._currentState != ArrayState.IN_ELEMENT)
				{
					throw new IllegalStateException("Attempted to end an array element in array that has not started and element");
				}

				// update internal state
				this._currentState = ArrayState.IN_ARRAY;

				// fire element type creation event
				context.addElement(this.getElementTypeName(), this.getElementType());
				break;

			default:
				throw new IllegalStateException("Unsupported event type: " + event.name());
		}
	}
}
