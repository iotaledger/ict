import React, { Component } from 'react';

import { get, set } from '../lib/api';

import { moduleURI } from '../lib/helpers';
import availableModules from '../lib/modules';
import Popup from '../components/Popup';
import Icon from '../components/Icon';

const defaultState = {
	modules: [],
	activeModule: null,
	installModule: null,
	installing: false,
	installError: null,
	removeModule: null,
	removing: null,
	removingError: null,
	updateModule: false,
	updating: false,
	updatingError: null
};

class Modules extends Component {
	state = Object.assign({}, defaultState);

	componentDidMount() {
		this.init();
	}

	init = async () => {
		const { modules } = await get('modules');
		this.setState(Object.assign({}, defaultState, { modules }));
	};

	setModule = (nextModule) => () => {
		const { activeModule } = this.state;

		this.setState({
			activeModule: activeModule !== nextModule ? nextModule : null
		});
	};

	installModule = async (e) => {
		e.preventDefault();

		const { installModule } = this.state;

		this.setState({
			installing: true
		});

		const { error } = await set('addModule', { user_slash_repo: installModule });

		if (!error) {
			this.init();
		} else {
			this.setState({
				installError: error,
				installing: false
			});
		}
	};

	updateModule = async () => {
		const { path, version } = this.state.updateModule;

		this.setState({
			updating: true
		});

		const { error } = await set('updateModule', { path, version });

		if (!error) {
			this.init();
		} else {
			this.setState({
				updatingError: error,
				updating: false
			});
		}
	};

	removeModule = async () => {
		const { removeModule } = this.state;

		this.setState({
			removing: true
		});

		const { error } = await set('removeModule', { path: removeModule });

		if (!error) {
			this.init();
		} else {
			this.setState({
				removingError: error,
				removing: false
			});
		}
	};

	render() {
		const {
			activeModule,
			modules,
			installModule,
			removeModule,
			removing,
			installing,
			installError,
			removingError,
			updating,
			updateModule,
			updatingError
		} = this.state;

		const installedModules = modules.map(({ name }) => name);

		return (
			<section className="modules">
				<article>
					<h1>
						Manage Modules{' '}
						<nav>
							<button className="button secondary" onClick={() => this.setState({ installModule: '' })} type="button">
								Install third party module
							</button>
						</nav>
					</h1>

					{typeof installModule === 'string' && (
						<Popup
							title="Install module"
							loading={installing}
							onClose={() => this.setState({ installModule: null, installError: null })}
						>
							<form onSubmit={this.installModule}>
								<fieldset>
									<p>
										<strong>Do not install modules from untrusted sources!</strong>
										<br />
										Format: <strong>username/repository</strong> or Github URL
									</p>
									<label htmlFor="newAddress">
										GitHub repository
										<input
											type="text"
											id="newAddress"
											value={installModule}
											onChange={(e) => this.setState({ installModule: e.target.value })}
										/>
									</label>
									{installError && <small className="error">{installError}</small>}
									<button className="button" type="submit">
										Install
									</button>
								</fieldset>
							</form>
						</Popup>
					)}

					{removeModule && (
						<Popup
							onConfirm={this.removeModule}
							type="warning"
							cta="Remove"
							loading={removing}
							onClose={() => this.setState({ removeModule: null, removing: null, removingError: null })}
						>
							<p>
								Remove module <strong>{removeModule}</strong>?
								{removingError && <small className="error">{removingError}</small>}
							</p>
						</Popup>
					)}

					{updateModule && (
						<Popup
							onConfirm={this.updateModule}
							type="success"
							cta="Update"
							loading={updating}
							onClose={() => this.setState({ updateModule: null, updating: null, updatingError: null })}
						>
							<p>
								Update module <strong>{updateModule.path}</strong> to version <strong>{updateModule.version}</strong>?
								{updatingError && <small className="error">{updatingError}</small>}
							</p>
						</Popup>
					)}

					{modules.length > 0 && (
						<div className="module-list">
							<h3>Installed</h3>
							<ul>
								{modules.map(({ name, repository, update, path, description, gui_port }) => (
									<li onClick={this.setModule(name)} key={path} className={activeModule === name ? 'active' : ''}>
										<h4>
											<Icon size={20} icon="info" />
											<Icon size={20} icon="close" />
											{name}
											<nav>
												<a
													href={moduleURI(name, gui_port)}
													className="button success small"
													target="_blank"
													rel="noopener noreferrer"
												>
													Launch
												</a>
												{update && (
													<button
														className="button small"
														onClick={() => this.setState({ updateModule: { path, version: update } })}
														type="button"
													>
														Update
													</button>
												)}
												<button
													className="button warning small"
													onClick={() => this.setState({ removeModule: path })}
													type="button"
												>
													Remove
												</button>
											</nav>
										</h4>
										<p>{description}</p>
										<a href={`https://github.com/${repository}`} target="_blank" rel="noopener noreferrer">
											<Icon size={12} icon="link" />
											Visit GitHub Repo
										</a>
									</li>
								))}
							</ul>
						</div>
					)}
					<div className="module-list">
						<h3>Supported modules</h3>
						<ul>
							{availableModules.map(
								({ name, repo, description }) =>
									installedModules.indexOf(name) < 0 && (
										<li onClick={this.setModule(name)} key={repo} className={activeModule === name ? 'active' : ''}>
											<h4>
												<Icon size={20} icon="info" />
												<Icon size={20} icon="close" />
												{name}
												<nav>
													<button
														className="button small success"
														onClick={(e) => {
															e.stopPropagation();
															this.setState({ installModule: repo });
														}}
														type="button"
													>
														Install
													</button>
												</nav>
											</h4>
											<p>{description}</p>
											<a
												href={`https://github.com/${repo}`}
												onClick={(e) => e.stopPropagation()}
												target="_blank"
												rel="noopener noreferrer"
											>
												<Icon size={12} icon="link" />
												Visit GitHub Repo
											</a>
										</li>
									)
							)}
						</ul>
					</div>
				</article>
			</section>
		);
	}
}

export default Modules;
