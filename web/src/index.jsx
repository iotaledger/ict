import React, { PureComponent, Fragment } from 'react';
import ReactDOM from 'react-dom';
import { Switch, Route } from 'react-router';
import { BrowserRouter as Router } from 'react-router-dom';
import Cookies from 'js-cookie';

import { get } from './lib/api';

import Sidebar from './components/Sidebar';
import Popup from './components/Popup';

import Home from './pages/Home';
import Neighbors from './pages/Neighbors';
import Configuration from './pages/Configuration';
import Log from './pages/Log';
import Modules from './pages/Modules';
import Module from './pages/Module';

import './style/index.scss';

class Main extends PureComponent {
	state = {
		authorised: null,
		loading: false,
		password: '',
		modules: []
	};

	componentDidMount() {
		this.authorise();
		this.updateModules();
	}

	updateModules = async () => {
		const { modules } = await get('modules');
		if (modules) {
			this.setState({ modules });
		}
	};

	authorise = async (e) => {
		const { password, authorised } = this.state;

		if (e) {
			e.preventDefault();
		}

		this.setState({
			loading: true
		});

		const { modules, error } = await get('modules', password);

		this.setState({
			loading: false
		});

		if (error && authorised) {
			this.setState({
				authorised: error
			});
		} else if (modules) {
			this.setState({
				authorised: 'authorised'
			});
			if (password.length) {
				Cookies.set('password', password);
			}
		} else {
			this.setState({
				authorised: 'init'
			});
		}
	};

	render() {
		const { authorised, password, loading, modules } = this.state;

		if (!authorised) return null;

		const renderMergedProps = (component, ...rest) => {
			const finalProps = Object.assign({}, ...rest);
			return React.createElement(component, finalProps);
		};

		const ModulesRoute = ({ component, ...rest }) => {
			return (
				<Route
					{...rest}
					render={(routeProps) => {
						return renderMergedProps(component, routeProps, rest);
					}}
				/>
			);
		};

		if (authorised !== 'authorised') {
			return (
				<Popup title="ICT">
					<form onSubmit={this.authorise}>
						<fieldset>
							<p>
								The password is set in your ict.cfg. The default value is <strong>change_me_now</strong>
							</p>
							<label htmlFor="password">
								Password
								<input
									type="password"
									id="password"
									value={password}
									loading={loading}
									onChange={(e) => this.setState({ password: e.target.value })}
								/>
							</label>
							{authorised !== 'init' && <small className="error">{authorised}</small>}
						</fieldset>
						<fieldset className="confirm">
							<input className="button" type="submit" value="Login" />
						</fieldset>
					</form>
				</Popup>
			);
		}

		return (
			<Router>
				<Fragment>
					<Sidebar modules={modules} />
					<Switch>
						<Route exact path="/" component={Home} />
						<Route exact path="/neighbors" component={Neighbors} />
						<Route exact path="/configuration" component={Configuration} />
						<Route exact path="/log" component={Log} />
						<ModulesRoute exact path="/modules" component={Modules} modules={modules} updateModules={this.updateModules} />
						<Route exact path="/modules/:path" component={Module} />
					</Switch>
				</Fragment>
			</Router>
		);
	}
}

ReactDOM.render(<Main />, document.getElementById('app'));
