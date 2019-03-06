import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import { get } from '../lib/api';
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

class Sidebar extends Component {
	state = {
		modules: [],
		menuActive: false
	};

	componentDidMount() {
		this.init();
	}

	init = async () => {
		const { modules } = await get('modules');
		if (modules) {
			this.setState({ modules });
		}
	};

	closeMenu = () => {
		this.setState({
			menuActive: false
		});
	};

	toggleMenu = () => {
		const { menuActive } = this.state;

		this.setState({
			menuActive: !menuActive
		});
	};

	render() {
		const { menuActive, modules } = this.state;

		return (
			<aside className={menuActive ? 'open' : ''}>
				<header>
					<img src={Logo} alt="IOTA Controlled Agent" />
					<button type="button" onClick={this.toggleMenu}>
						<Icon icon="menu" size={25} />
					</button>
				</header>
				<div>
					<h1>General</h1>
					<nav onClick={this.closeMenu}>
						<Link to="/" label="Home" />
						<Link to="/neighbors" label="Neighbors" />
						<Link to="/configuration" label="Configuration" />
						<Link to="/log" label="Log" />
					</nav>
					<h1>IXI Modules</h1>
					<nav onClick={this.closeMenu}>
						<Link to="/modules" label="Manage modules" />
						{modules.map(({ path, name }) => (
							<Link key={path} to={`/modules/${path}`} label={name} />
						))}
					</nav>
				</div>
			</aside>
		);
	}
}

export default Sidebar;
