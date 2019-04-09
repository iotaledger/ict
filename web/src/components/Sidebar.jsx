import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import Icon from './Icon';

import Logo from '../assets/ict-logo.svg';

const Link = ({ to, label }) => (
	<NavLink exact to={to}>
		<Icon icon={label} size={18} />
		{label}
	</NavLink>
);

Link.propTypes = {
	to: PropTypes.string.isRequired,
	label: PropTypes.string.isRequired
};

const Sidebar = ({ modules }) => {
	const [active, setActive] = useState(false);

	return (
		<aside className={active ? 'open' : ''}>
			<header>
				<img src={Logo} alt="IOTA Controlled Agent" />
				<button type="button" onClick={() => setActive(!active)}>
					<Icon icon="menu" size={25} />
				</button>
			</header>
			<div>
				<h1>General</h1>
				<nav onClick={() => setActive(false)}>
					<Link to="/" label="Home" />
					<Link to="/neighbors" label="Neighbors" />
					<Link to="/configuration" label="Configuration" />
					<Link to="/log" label="Log" />
				</nav>
				<h1>IXI Modules</h1>
				<nav onClick={() => setActive(false)}>
					<Link to="/modules" label="Manage modules" />
					{modules.map(({ path, name }) => (
						<Link key={path} to={`/modules/${path}`} label={name} />
					))}
				</nav>
			</div>
		</aside>
	);
};

Sidebar.propTypes = {
	modules: PropTypes.array.isRequired
};

export default Sidebar;
