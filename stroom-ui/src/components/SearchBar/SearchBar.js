import React from 'react';
import PropTypes from 'prop-types';
import {
  Container,
  Input,
  Button,
  Grid,
  Message,
} from 'semantic-ui-react';
import { compose, withState } from 'recompose';
import { connect } from 'react-redux';

import Tooltip from 'components/Tooltip';

import {
  ExpressionBuilder,
  actionCreators as expressionBuilderActionCreators,
} from 'components/ExpressionBuilder';

import { processSearchString } from './searchBarUtils';

const { expressionChanged } = expressionBuilderActionCreators;

const withIsExpression = withState('isExpression', 'setIsExpression', false);

const withSearchString = withState('searchString', 'setSearchString', '');
const withExpression = withState('expression', 'setExpression', '');

const withIsSearchStringValid = withState('isSearchStringValid', 'setIsSearchStringValid', true);
const withSearchStringValidationMessages = withState(
  'searchStringValidationMessages',
  'setSearchStringValidationMessages',
  [],
);

const enhance = compose(
  connect(
    (state, props) => ({
      expression: state.expressionBuilder.expressions[props.expressionId],
    }),
    { expressionChanged },
  ),
  withIsExpression,
  withSearchString,
  withExpression,
  withIsSearchStringValid,
  withSearchStringValidationMessages,
);

const SearchBar = ({
  dataSource,
  expressionId,
  expressionChanged,
  searchString,
  isExpression,
  setIsExpression,
  setSearchString,
  expression,
  setExpression,
  setIsSearchStringValid,
  isSearchStringValid,
  setSearchStringValidationMessages,
  searchStringValidationMessages,
  onSearch,
}) => {
  const searchIsInvalid = searchStringValidationMessages.length > 0;
  const searchButton = (
    <Button
      disabled={searchIsInvalid}
      className="icon-button"
      icon="search"
      onClick={() => {
        onSearch(expressionId);
      }}
    />
  );
  const searchInput = (
    <React.Fragment>
      <Grid className="SearchBar__layoutGrid">
        <Grid.Row>
          <Grid.Column width={1}>
            <Tooltip
              trigger={
                <Button
                  className="icon-button"
                  disabled={searchIsInvalid}
                  circular
                  icon="edit"
                  onClick={() => {
                    const parsedExpression = processSearchString(dataSource, searchString);
                    expressionChanged(expressionId, parsedExpression.expression);

                    setIsExpression(true);
                  }}
                />
              }
              content={<React.Fragment><p>Switch to using the expression builder.</p> <p>You won't be able to convert back to a text search and keep your expression.</p></React.Fragment>}
            />
          </Grid.Column>
          <Grid.Column width={12}>
            <Input
              placeholder="I.e. field1=value1 field2=value2"
              value={searchString}
              className="SearchBar__input"
              onChange={(_, data) => {
                const expression = processSearchString(dataSource, data.value);
                const invalidFields = expression.fields.filter(field => !field.conditionIsValid || !field.fieldIsValid || !field.valueIsValid);

                const searchStringValidationMessages = [];
                if (invalidFields.length > 0) {
                  invalidFields.forEach((invalidField) => {
                    searchStringValidationMessages.push(`'${invalidField.original}' is not a valid search term`);
                  });
                }

                setIsSearchStringValid(invalidFields.length === 0);
                setSearchStringValidationMessages(searchStringValidationMessages);
                setSearchString(data.value);

                const parsedExpression = processSearchString(dataSource, searchString);
                expressionChanged(expressionId, parsedExpression.expression);
              }}
            />
          </Grid.Column>
          <Grid.Column width={2}>{searchButton}</Grid.Column>
        </Grid.Row>
        {searchIsInvalid ? (
          <Grid.Row>
            <Grid.Column width={1} />
            <Grid.Column width={12}>
              <Container>
                <Message warning className="SearchBar__validationMessages">
                  {searchStringValidationMessages.map((message, i) => (
                    <p key={i}>{message}</p>
                  ))}
                </Message>
              </Container>
            </Grid.Column>
            <Grid.Column width={2} />
          </Grid.Row>
        ) : (
          undefined
        )}
      </Grid>
    </React.Fragment>
  );

  const expressionBuilder = (
    <React.Fragment>
      <Grid className="SearchBar__layoutGrid">
        <Grid.Column width={1}>
          <Tooltip
            trigger={
              <Button
                circular
                icon="text cursor"
                className="SearchBar__modeButton icon-button"
                onClick={() => setIsExpression(false)}
              />
            }
            content="Switch to using text search. You'll lose the expression you've built here."
          />
        </Grid.Column>
        <Grid.Column width={13}>
          <ExpressionBuilder
            className="SearchBar__expressionBuilder"
            showModeToggle={false}
            editMode
            dataSource={dataSource}
            expressionId={expressionId}
          />
        </Grid.Column>
        <Grid.Column width={2}>{searchButton}</Grid.Column>
      </Grid>
    </React.Fragment>
  );

  return <div className="SearchBar flat">{isExpression ? expressionBuilder : searchInput}</div>;
};

SearchBar.propTypes = {
  dataSource: PropTypes.object.isRequired,
  expressionId: PropTypes.string.isRequired,
  searchString: PropTypes.string,
  onSearch: PropTypes.func.isRequired,
};

export default enhance(SearchBar);