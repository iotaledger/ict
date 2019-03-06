import React from 'react';
import PropTypes from 'prop-types';

const Card = ({ title, children, columns }) => (
	<article className="card">
		<h2>{title}</h2>
		<div className={columns ? 'columns' : ''}>{children}</div>
	</article>
);

Card.propTypes = {
	title: PropTypes.string,
	children: PropTypes.node.isRequired,
	columns: PropTypes.bool
};

Card.defaultProps = {
	title: '',
	columns: false
};

export default Card;
